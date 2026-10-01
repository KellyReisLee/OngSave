package br.com.ongsave.service;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import br.com.ongsave.dao.DocumentoDAO;
import br.com.ongsave.dao.EmpresaDAO;
import br.com.ongsave.dao.LoteDAO;
import br.com.ongsave.dao.MovimentoDAO;
import br.com.ongsave.dao.OcorrenciaDAO;
import br.com.ongsave.dao.OngDAO;
import br.com.ongsave.dao.UsuarioDAO;
import br.com.ongsave.db.Banco;
import br.com.ongsave.db.Sql;
import br.com.ongsave.model.Documento;
import br.com.ongsave.model.Empresa;
import br.com.ongsave.model.ErroNegocio;
import br.com.ongsave.model.Lote;
import br.com.ongsave.model.Ocorrencia;
import br.com.ongsave.model.Ong;
import br.com.ongsave.model.Usuario;
import br.com.ongsave.util.Codigos;
import br.com.ongsave.util.Documentos;
import br.com.ongsave.util.Imagens;
import br.com.ongsave.util.Json;

/**
 * Painel da ONG: propostas de doação (aceitar/recusar), chegadas, token de validação,
 * conferência do recebimento (libera ou retém o frete), perfil e documentos.
 */
public class OngService {

    /** Distância a partir da qual o motorista é considerado "na porta". */
    private static final double PORTA_M = 200;
    private static final int TOKEN_MIN = 10;
    private static final long DOC_MAX = 5L * 1024 * 1024;

    private final PlataformaService plataforma;
    private final NotificacaoService notificacoes;
    private final AuditoriaService auditoria;
    private final Geocodificador geo;
    private final boolean geoValidar;
    /** No modo simulação a ONG vê "a caminho"/"na porta" pela distância real do motorista simulado. */
    private final boolean simulacao;
    private final OngDAO ongs = new OngDAO();
    private final LoteDAO lotes = new LoteDAO();
    private final EmpresaDAO empresas = new EmpresaDAO();
    private final UsuarioDAO usuarios = new UsuarioDAO();
    private final MovimentoDAO movimentos = new MovimentoDAO();
    private final OcorrenciaDAO ocorrencias = new OcorrenciaDAO();
    private static final DocumentoDAO DOCUMENTOS = new DocumentoDAO();

    public OngService(PlataformaService plataforma, NotificacaoService notificacoes, AuditoriaService auditoria,
                      Geocodificador geo, boolean geoValidar, boolean simulacao) {
        this.simulacao = simulacao;
        this.plataforma = plataforma;
        this.notificacoes = notificacoes;
        this.auditoria = auditoria;
        this.geo = geo;
        this.geoValidar = geoValidar;
    }

    /* ============================ Leitura ============================ */

    public Map<String, Object> estado(Usuario u) {
        return Banco.ler(c -> {
            Map<String, Object> r = new LinkedHashMap<>();
            Map<String, Object> ong = perfil(c, u.getId());
            r.put("ong", ong);

            List<Map<String, Object>> doacoes = new ArrayList<>();
            for (Map<String, Object> l : lotes.detalhesDaOng(c, u.getId()))
                doacoes.add(doacao(c, l));
            r.put("doacoes", doacoes);

            List<Map<String, Object>> disp = new ArrayList<>();
            for (Map<String, Object> l : lotes.propostasDaOng(c, u.getId())) {
                double km = br.com.ongsave.util.Geo.km(Sql.d(l, "empLat"), Sql.d(l, "empLon"), Sql.d(l, "ongLat"), Sql.d(l, "ongLon"));
                disp.add(Json.obj("id", l.get("id"), "tipo", l.get("tipo"), "kg", l.get("kg"), "emp", l.get("emp"), "cons", l.get("cons"),
                        "dist", Math.round(km * 10) / 10.0, "h", Lotes.horas(l), "aceita", l.get("ongAceiteMs")));
            }
            r.put("disp", disp);

            List<Estatisticas.Ponto> pts = new ArrayList<>();
            for (Map<String, Object> p : lotes.entregasDaOng(c, u.getId()))
                pts.add(new Estatisticas.Ponto(Instant.parse(Sql.s(p, "entregue_em")), Sql.d(p, "kg"), Sql.l(p, "empresa_id"), Sql.s(p, "categoria")));
            r.put("per", Estatisticas.periodos(pts));
            r.put("categorias", Estatisticas.categorias(pts));

            List<List<Object>> parceiros = new ArrayList<>();
            for (Map<String, Object> p : lotes.principaisDoadores(c, u.getId()))
                parceiros.add(List.of(p.get("nome"), p.get("kg")));
            r.put("parceiros", parceiros);
            r.put("docs", documentos(c, u.getId()));
            r.put("notifs", notificacoes.listar(c, u.getId()));
            return r;
        });
    }

    private Map<String, Object> perfil(Connection c, long id) throws SQLException {
        Map<String, Object> o = ongs.perfilPainel(c, id);
        if (o == null) throw ErroNegocio.naoEncontrado("Instituição não encontrada.");
        o.put("horario", o.get("horaIni") + " às " + o.get("horaFim"));
        o.put("end", o.get("rua") + ", " + o.get("num") + (Sql.s(o, "compl").isEmpty() ? "" : " - " + o.get("compl"))
                + " - " + o.get("bairro") + ", " + o.get("cidade") + "/" + o.get("uf"));
        if (o.get("alvaraDias") == null) o.put("alvaraDias", 0);
        return o;
    }

    /** Linha do histórico da ONG no formato do ong.js (estado: caminho | porta | recebida). */
    private Map<String, Object> doacao(Connection c, Map<String, Object> l) throws SQLException {
        String estado = switch (Sql.s(l, "estado")) {
            case "entregue" -> "recebida";
            case "transito" -> {
                double m = Lotes.metrosAteOng(l);
                yield !(geoValidar || simulacao) || (!Double.isNaN(m) && m <= PORTA_M) ? "porta" : "caminho";
            }
            default -> "caminho";
        };
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", l.get("id"));
        m.put("tipo", l.get("tipo"));
        m.put("kg", l.get("kg"));
        m.put("emp", l.get("emp"));
        m.put("empLat", l.get("empLat"));
        m.put("empLon", l.get("empLon"));
        m.put("cons", l.get("cons"));
        m.put("estado", estado);
        m.put("fase", l.get("estado"));
        m.put("prog", "aceito".equals(l.get("estado")) ? 0 : Lotes.progresso(l));
        m.put("posLat", l.get("posLat"));
        m.put("posLon", l.get("posLon"));
        m.put("mot", l.get("mot"));
        m.put("veic", Lotes.veiculoEmpresa(l));
        m.put("placa", l.get("placa") == null ? "" : l.get("placa"));
        m.put("h", Lotes.horas(l));
        m.put("dias", l.get("dias"));
        m.put("pesoOrigem", l.get("retKg"));
        m.put("conf", Lotes.conferencia(l));
        m.put("token", Lotes.token(l));
        m.put("nova", false);
        if ("recebida".equals(estado) && Boolean.TRUE.equals(l.get("temFoto")))
            m.put("foto", Json.obj("em", l.get("entregueMs"), "dist", l.get("fotoDist") == null ? "—" : l.get("fotoDist"), "url", true));
        return m;
    }

    private static List<Map<String, Object>> documentos(Connection c, long id) throws SQLException {
        return DOCUMENTOS.listarDoUsuario(c, id);
    }

    /* ============================ Propostas ============================ */

    public void aceitarProposta(Usuario u, long loteId) {
        Banco.transacao(c -> {
            Lote l = lotes.buscarPorId(c, loteId, true);
            if (l == null || l.getOngId() == null || l.getOngId() != u.getId() || !"aguardando".equals(l.getEstado()))
                throw ErroNegocio.naoEncontrado("Proposta não encontrada ou já não está disponível.");
            if (l.isAceitoPelaOng()) return null;
            Ong o = ongs.buscar(c, u.getId());
            if (o == null) throw ErroNegocio.naoEncontrado("Instituição não encontrada.");
            if (o.getCategorias() == null || !o.getCategorias().contains(l.getCategoria()))
                throw ErroNegocio.invalido("Categoria fora das que a instituição aceita.");
            if (!"amb".equals(l.getConservacao()) && !o.isCamaraFria())
                throw ErroNegocio.invalido("Este lote exige câmara fria, que a instituição não tem.");
            double previsto = ongs.kgPrevistoHoje(c, u.getId());
            if (previsto + l.getPesoKg() > o.getCapacidadeKg())
                throw ErroNegocio.invalido("Passa da capacidade diária da instituição (" + EmpresaService.fmt(o.getCapacidadeKg()) + " kg).");
            lotes.aceitarPelaOng(c, loteId);
            notificacoes.notificar(c, l.getEmpresaId(), "A " + u.getNome() + " aceitou o lote #" + loteId
                    + ". Os motoristas da região já podem levá-lo.", "fa-handshake");
            auditoria.registrar(c, u, "Aceitou proposta", "#" + loteId, loteId, null);
            return null;
        });
    }

    /** Recusa: o lote vai automaticamente para a próxima ONG compatível mais próxima. */
    public void recusarProposta(Usuario u, long loteId, String motivo) {
        Banco.transacao(c -> {
            Lote l = lotes.buscarPorId(c, loteId, true);
            if (l == null || l.getOngId() == null || l.getOngId() != u.getId() || !"aguardando".equals(l.getEstado()))
                throw ErroNegocio.naoEncontrado("Proposta não encontrada ou já não está disponível.");
            if (l.getMotoristaId() != null) throw ErroNegocio.conflito("Um motorista já aceitou este lote.");
            lotes.registrarRecusa(c, loteId, u.getId());
            Empresa e = empresas.buscar(c, l.getEmpresaId());
            double lat = e == null || e.getLat() == null ? -23.5505 : e.getLat();
            double lon = e == null || e.getLon() == null ? -46.6333 : e.getLon();
            Long nova = EmpresaService.escolherOng(c, "auto", l.getCategoria(), l.getConservacao(), lat, lon, loteId);
            lotes.reatribuirOng(c, loteId, nova);
            String m = EmpresaService.t(motivo);
            auditoria.registrar(c, u, "Recusou proposta", "#" + loteId, loteId, m.isEmpty() ? null : m);
            long emp = l.getEmpresaId();
            if (nova != null) {
                String nomeNova = usuarios.nome(c, nova);
                notificacoes.notificar(c, nova, "Nova proposta de doação: lote #" + loteId + " (" + l.getCategoria() + "). Aceite ou recuse no painel.", "fa-box-open");
                notificacoes.notificar(c, emp, "A " + u.getNome() + " não pôde receber o lote #" + loteId + ". A proposta foi enviada à " + nomeNova + ".", "fa-arrows-rotate");
            } else {
                notificacoes.notificar(c, emp, "A " + u.getNome() + " não pôde receber o lote #" + loteId
                        + " e não há outra ONG compatível agora. A equipa OngSave foi avisada.", "fa-triangle-exclamation");
                notificacoes.notificarAdmins(c, "Lote #" + loteId + " ficou sem ONG compatível após recusa da " + u.getNome() + ".", "fa-triangle-exclamation");
            }
            return null;
        });
    }

    /* ============================ Entrega ============================ */

    /** Gera (ou renova) o token que o motorista digita para validar a entrega. Um novo invalida o anterior. */
    public Map<String, Object> gerarToken(Usuario u, long loteId) {
        return Banco.transacao(c -> {
            Map<String, Object> l = lotes.detalheDaOng(c, loteId, u.getId());
            if (l == null) throw ErroNegocio.naoEncontrado("Lote não encontrado.");
            if (!"transito".equals(l.get("estado")))
                throw ErroNegocio.conflito("aceito".equals(l.get("estado"))
                        ? "O motorista ainda não coletou o lote na empresa." : "Este lote já não está a caminho.");
            if (geoValidar) {
                double m = Lotes.metrosAteOng(l);
                if (Double.isNaN(m) || m > PORTA_M * 2)
                    throw ErroNegocio.conflito("O motorista ainda não chegou à instituição.");
            }
            String codigo = Codigos.gerar(6);
            lotes.gerarToken(c, loteId, codigo, TOKEN_MIN);
            notificacoes.notificar(c, Sql.l(l, "motoristaId"), "A " + u.getNome() + " gerou o token do lote #" + loteId
                    + ". Peça o código no local e digite-o no app com a foto da descarga.", "fa-key");
            long agora = System.currentTimeMillis();
            return Json.obj("codigo", codigo, "em", agora, "expira", agora + TOKEN_MIN * 60_000L, "usado", false);
        });
    }

    /**
     * Conferência do recebimento. Diferença de peso acima da tolerância ou alimento impróprio abre ocorrência
     * e mantém o frete retido até a análise da administração; caso contrário o frete é liberado ao motorista.
     */
    public Map<String, Object> conferencia(Usuario u, long loteId, String kgTxt, String cond, String obsTxt) {
        double kg = EmpresaService.numero(kgTxt, "Informe o peso recebido.");
        if (kg < 0 || kg > 100_000) throw ErroNegocio.invalido("Informe o peso recebido.");
        if (!List.of("ok", "parcial", "improprio").contains(cond)) throw ErroNegocio.invalido("Informe a condição dos alimentos.");
        String obs = EmpresaService.t(obsTxt);
        if (obs.length() > 300) throw ErroNegocio.invalido("A observação passa de 300 caracteres.");
        double tol = plataforma.tolPesoPct();

        return Banco.transacao(c -> {
            Lote l = lotes.buscarPorId(c, loteId, true);
            if (l == null || l.getOngId() == null || l.getOngId() != u.getId()) throw ErroNegocio.naoEncontrado("Lote não encontrado.");
            if (!"entregue".equals(l.getEstado())) throw ErroNegocio.conflito("A entrega ainda não foi validada.");
            if (l.getConfKg() != null) throw ErroNegocio.conflito("Este recebimento já foi conferido.");

            double origem = l.pesoDeOrigem();
            double div = origem > 0 ? Math.abs(kg - origem) / origem * 100 : 0;
            boolean peso = div > tol, ruim = !"ok".equals(cond), retido = peso || ruim;
            double divReg = peso ? Math.round(div * 10) / 10.0 : 0;

            lotes.registrarConferencia(c, loteId, kg, cond, obs.isEmpty() ? null : obs, divReg, retido);
            Long mot = l.getMotoristaId();
            if (retido) {
                String tipo = ruim ? "Alimento impróprio" : "Peso divergente";
                String texto = (ruim ? "Condição informada: " + ("parcial".equals(cond) ? "parcialmente impróprio" : "impróprio") + ". " : "")
                        + "Peso na retirada " + EmpresaService.fmt(origem) + " kg, recebido " + EmpresaService.fmt(kg) + " kg ("
                        + String.format(java.util.Locale.ROOT, "%.1f", div) + "%). " + obs;
                ocorrencias.inserir(c, ocorrencia(loteId, tipo, AuditoriaService.corta(texto.trim(), 600), u.getId()));
                notificacoes.notificarAdmins(c, "Nova ocorrência (" + tipo + ") no lote #" + loteId + " reportada pela " + u.getNome() + ".", "fa-triangle-exclamation");
                notificacoes.notificar(c, mot, "O frete do lote #" + loteId + " ficou retido: a ONG registou " + tipo.toLowerCase()
                        + ". A administração vai analisar.", "fa-scale-balanced");
                notificacoes.notificar(c, l.getEmpresaId(), "A " + u.getNome() + " registou " + tipo.toLowerCase() + " no lote #" + loteId + ".", "fa-triangle-exclamation");
                notificacoes.notificar(c, u.getId(), "Ocorrência do lote #" + loteId + " enviada à administração. O frete do motorista fica retido até a análise.", "fa-triangle-exclamation");
            } else {
                movimentos.liberarFreteDoLote(c, loteId);
                notificacoes.notificar(c, mot, "Recebimento do lote #" + loteId + " conferido. Frete liberado na sua carteira.", "fa-sack-dollar");
                notificacoes.notificar(c, u.getId(), "Recebimento do lote #" + loteId + " conferido. Frete liberado ao motorista.", "fa-clipboard-check");
            }
            auditoria.registrar(c, u, "Conferiu recebimento", "#" + loteId, loteId, EmpresaService.fmt(kg) + " kg · " + cond);
            return Json.obj("kg", kg, "cond", cond, "obs", obs, "div", divReg, "retido", retido);
        });
    }

    /** "Algo não confere" antes de gerar o token: abre ocorrência para a administração. */
    public void reportar(Usuario u, long loteId, String texto) {
        String t = EmpresaService.t(texto);
        if (t.length() > 600) throw ErroNegocio.invalido("O texto passa de 600 caracteres.");
        Banco.transacao(c -> {
            Lote l = lotes.buscarPorId(c, loteId, false);
            if (l == null || l.getOngId() == null || l.getOngId() != u.getId())
                throw ErroNegocio.naoEncontrado("Lote não encontrado.");
            ocorrencias.inserir(c, ocorrencia(loteId, "Problema na chegada",
                    t.isEmpty() ? "Motorista ou carga não conferem com o registado." : t, u.getId()));
            notificacoes.notificarAdmins(c, "A " + u.getNome() + " reportou um problema na chegada do lote #" + loteId + ".", "fa-triangle-exclamation");
            notificacoes.notificar(c, u.getId(), "Problema com o lote #" + loteId + " reportado à plataforma. Aguarde o contato da administração.", "fa-triangle-exclamation");
            return null;
        });
    }

    private static Ocorrencia ocorrencia(long loteId, String tipo, String obs, long autorId) {
        Ocorrencia o = new Ocorrencia();
        o.setLoteId(loteId);
        o.setTipo(tipo);
        o.setObs(obs);
        o.setAutorId(autorId);
        return o;
    }

    /* ============================ Perfil ============================ */

    public Map<String, Object> atualizarPerfil(Usuario u, Map<String, String> d, List<String> categorias, boolean camaraFria) {
        String nome = EmpresaService.t(d.get("nome")), fantasia = EmpresaService.t(d.get("fantasia")), resp = EmpresaService.t(d.get("resp"));
        String email = EmpresaService.t(d.get("email")), tel = EmpresaService.t(d.get("tel"));
        if (nome.isEmpty() || nome.length() > 160) throw ErroNegocio.invalido("Informe a razão social.");
        if (fantasia.isEmpty() || fantasia.length() > 160) throw ErroNegocio.invalido("Informe o nome de exibição.");
        if (resp.isEmpty() || resp.length() > 120) throw ErroNegocio.invalido("Informe o responsável.");
        if (!email.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) throw ErroNegocio.invalido("Informe um e-mail válido.");
        if (!Documentos.telefoneValido(tel)) throw ErroNegocio.invalido("Informe um telefone com DDD.");
        int familias = (int) EmpresaService.numero(d.get("familias"), "Informe o número de famílias.");
        if (familias < 1 || familias > 1_000_000) throw ErroNegocio.invalido("Informe o número de famílias.");
        double cap = EmpresaService.numero(d.get("capKg"), "Informe a capacidade diária.");
        if (cap <= 0 || cap > 1_000_000) throw ErroNegocio.invalido("Informe a capacidade diária.");
        LocalTime ini, fim;
        try { ini = LocalTime.parse(EmpresaService.t(d.get("horaIni"))); fim = LocalTime.parse(EmpresaService.t(d.get("horaFim"))); }
        catch (DateTimeParseException e) { throw ErroNegocio.invalido("Informe o horário de recebimento."); }
        if (!ini.isBefore(fim)) throw ErroNegocio.invalido("O horário final deve ser depois do inicial.");
        LocalDate alvara;
        try { alvara = LocalDate.parse(EmpresaService.t(d.get("alvara"))); }
        catch (DateTimeParseException e) { throw ErroNegocio.invalido("Informe a validade do alvará."); }
        if (alvara.isBefore(LocalDate.now())) throw ErroNegocio.invalido("Informe uma validade futura para o alvará.");
        List<String> cats = new ArrayList<>();
        for (String cat : categorias) if (EmpresaService.CATEGORIAS.contains(cat) && !cats.contains(cat)) cats.add(cat);
        if (cats.isEmpty()) throw ErroNegocio.invalido("Marque ao menos uma categoria.");
        EmpresaService.Endereco end = EmpresaService.Endereco.de(d);

        Map<String, Object> atual = Banco.ler(c -> perfil(c, u.getId()));
        double[] coords = null;
        if (end.mudou(atual) || Boolean.TRUE.equals(atual.get("semLocal")))
            coords = geo.localizar(end.rua(), end.num(), end.bairro(), end.cidade(), end.uf(), end.cep());
        final double[] novas = coords;

        return Banco.transacao(c -> {
            if (usuarios.emailEmUsoPorOutro(c, email, u.getId()))
                throw ErroNegocio.conflito("Este e-mail já está em uso por outra conta.");
            usuarios.atualizarContato(c, u.getId(), fantasia, email, tel);
            Ong o = new Ong();
            o.setUsuarioId(u.getId());
            o.setRazaoSocial(nome);
            o.setResponsavel(resp);
            o.setFamilias(familias);
            o.setCapacidadeKg(cap);
            o.setHoraInicio(ini);
            o.setHoraFim(fim);
            o.setCamaraFria(camaraFria);
            o.setCategorias(cats);
            o.setAlvaraValidade(alvara);
            o.setCep(end.cep());
            o.setRua(end.rua());
            o.setNumero(end.num());
            o.setComplemento(end.compl());
            o.setBairro(end.bairro());
            o.setCidade(end.cidade());
            o.setUf(end.uf());
            o.setLat(novas == null ? null : novas[0]);
            o.setLon(novas == null ? null : novas[1]);
            ongs.atualizarPerfil(c, o);
            if (!alvara.toString().equals(atual.get("alvara")))
                auditoria.registrar(c, u, "Atualizou validade do alvará", fantasia, u.getId(), atual.get("alvara") + " → " + alvara);
            Map<String, Object> r = perfil(c, u.getId());
            r.put("geocodificado", novas != null);
            return r;
        });
    }

    /** Envio de documento (PDF/PNG/JPG até 5 MB): volta para "em análise" e avisa os administradores. */
    public List<Map<String, Object>> enviarDocumento(Usuario u, long docId, String nomeArquivo, byte[] conteudo) {
        return enviarDocumentoComum(u, docId, nomeArquivo, conteudo, notificacoes, auditoria);
    }

    /** Partilhado por todos os perfis (rota /api/conta/documentos/{id}). */
    public static List<Map<String, Object>> enviarDocumentoComum(Usuario u, long docId, String nomeArquivo, byte[] conteudo,
                                                     NotificacaoService notificacoes, AuditoriaService auditoria) {
        if (conteudo == null || conteudo.length == 0) throw ErroNegocio.invalido("Escolha um ficheiro.");
        if (conteudo.length > DOC_MAX) throw ErroNegocio.invalido("O arquivo passa de 5 MB.");
        String tipo = Imagens.detectar(conteudo);
        if (!List.of("application/pdf", "image/png", "image/jpeg").contains(tipo)) throw ErroNegocio.invalido("Envie um PDF, PNG ou JPG.");
        String nomeArq = nomeArquivo == null ? "documento" : nomeArquivo.replaceAll("[\\\\/:*?\"<>|\\r\\n]", "_");
        if (nomeArq.length() > 200) nomeArq = nomeArq.substring(nomeArq.length() - 200);
        final String arq = nomeArq;
        return Banco.transacao(c -> {
            Documento d = DOCUMENTOS.buscar(c, docId);
            if (d == null || d.getUsuarioId() != u.getId()) throw ErroNegocio.naoEncontrado("Documento não encontrado.");
            DOCUMENTOS.registrarEnvio(c, docId, arq, conteudo, tipo);
            notificacoes.notificarAdmins(c, u.getNome() + " enviou o documento \"" + d.getNome() + "\" para análise.", "fa-file-circle-check");
            notificacoes.notificar(c, u.getId(), "Documento \"" + d.getNome() + "\" enviado para análise da administração.", "fa-file-circle-check");
            auditoria.registrar(c, u, "Enviou documento", d.getNome(), u.getId(), arq);
            return documentos(c, u.getId());
        });
    }

}
