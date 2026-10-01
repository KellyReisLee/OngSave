package br.com.ongsave.service;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import br.com.ongsave.dao.EmpresaDAO;
import br.com.ongsave.dao.FaturaDAO;
import br.com.ongsave.dao.LoteDAO;
import br.com.ongsave.dao.MotoristaDAO;
import br.com.ongsave.dao.OngDAO;
import br.com.ongsave.dao.UsuarioDAO;
import br.com.ongsave.db.Banco;
import br.com.ongsave.db.Sql;
import br.com.ongsave.model.Empresa;
import br.com.ongsave.model.ErroNegocio;
import br.com.ongsave.model.Lote;
import br.com.ongsave.model.Perfil;
import br.com.ongsave.model.Usuario;
import br.com.ongsave.util.Codigos;
import br.com.ongsave.util.Documentos;
import br.com.ongsave.util.Geo;
import br.com.ongsave.util.Imagens;
import br.com.ongsave.util.Json;

/** Painel da empresa doadora: lotes, retirada, perfil, plano e relatórios. */
public class EmpresaService {

    static final List<String> CATEGORIAS = List.of("Laticínios e Frios", "Hortifrúti / Frutas e Verduras",
            "Padaria e Panificados", "Mercearia / Não Perecíveis", "Pratos Prontos / Marmitas");
    private static final Map<String, Integer> CAPACIDADE = Map.of("carro", 80, "van", 300, "cam", 100_000);

    private final PlataformaService plataforma;
    private final NotificacaoService notificacoes;
    private final AuditoriaService auditoria;
    private final Geocodificador geo;
    private final EmpresaDAO empresas = new EmpresaDAO();
    private final LoteDAO lotes = new LoteDAO();
    private final OngDAO ongsDao = new OngDAO();
    private final MotoristaDAO motoristas = new MotoristaDAO();
    private final FaturaDAO faturas = new FaturaDAO();
    private final UsuarioDAO usuarios = new UsuarioDAO();

    public EmpresaService(PlataformaService plataforma, NotificacaoService notificacoes, AuditoriaService auditoria, Geocodificador geo) {
        this.plataforma = plataforma;
        this.notificacoes = notificacoes;
        this.auditoria = auditoria;
        this.geo = geo;
    }

    /* ============================ Leitura ============================ */

    /** Tudo o que as páginas da empresa precisam, num só objeto (injetado na JSP e devolvido em /api/empresa/estado). */
    public Map<String, Object> estado(Usuario u) {
        return Banco.transacao(c -> {
            garantirFaturaDoMes(c, u.getId());
            Map<String, Object> r = new LinkedHashMap<>();
            Map<String, Object> emp = perfil(c, u.getId());
            r.put("empresa", emp);

            List<Map<String, Object>> listaLotes = new ArrayList<>();
            for (Map<String, Object> l : lotes.detalhesDaEmpresa(c, u.getId()))
                listaLotes.add(lote(l));
            r.put("lotes", listaLotes);

            Map<String, Object> ongs = new LinkedHashMap<>();
            List<Map<String, Object>> ongsLista = new ArrayList<>();
            for (Map<String, Object> o : ongsDao.listarAtivas(c)) {
                ongs.put(Sql.s(o, "nome"), List.of(o.get("lat"), o.get("lon")));
                ongsLista.add(Json.obj("id", o.get("id"), "nome", o.get("nome"),
                        "km", Math.round(Geo.km(Sql.d(emp, "lat"), Sql.d(emp, "lon"), Sql.d(o, "lat"), Sql.d(o, "lon")) * 10) / 10.0));
            }
            r.put("ongs", ongs);
            r.put("ongsLista", ongsLista);
            r.put("usoMes", lotes.contarDoMes(c, u.getId()));
            r.put("motoristasAtivos", motoristas.contarAtivosNaSemana(c));

            List<Estatisticas.Ponto> pts = pontos(c, u.getId());
            r.put("per", Estatisticas.periodos(pts));
            r.put("esg", Estatisticas.esg(pts));
            r.put("veiculos", lotes.veiculosDaEmpresa(c, u.getId()));
            r.put("faturas", faturas.listarDaEmpresa(c, u.getId()));
            r.put("notifs", notificacoes.listar(c, u.getId()));
            return r;
        });
    }

    private Map<String, Object> perfil(Connection c, long id) throws SQLException {
        Map<String, Object> e = empresas.perfilPainel(c, id);
        if (e == null) throw ErroNegocio.naoEncontrado("Empresa não encontrada.");
        return e;
    }

    private static Map<String, Object> lote(Map<String, Object> l) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", l.get("id"));
        m.put("tipo", l.get("tipo"));
        m.put("kg", l.get("kg"));
        m.put("veic", Lotes.veiculoEmpresa(l));
        m.put("cons", l.get("cons"));
        m.put("estado", l.get("estado"));
        m.put("ong", l.get("ong") == null ? "A definir" : l.get("ong"));
        m.put("ongAceitou", l.get("ongAceitou"));
        m.put("ongLat", l.get("ongLat"));
        m.put("ongLon", l.get("ongLon"));
        m.put("mot", l.get("mot"));
        m.put("placa", l.get("placa"));
        m.put("prog", Lotes.progresso(l));
        m.put("posLat", l.get("posLat"));
        m.put("posLon", l.get("posLon"));
        m.put("dias", l.get("dias"));
        m.put("h", Lotes.horas(l));
        m.put("validade", l.get("validade"));
        m.put("entregueMs", l.get("entregueMs"));
        m.put("retirada", Lotes.retirada(l));
        m.put("pesoOrigem", l.get("retKg"));
        m.put("conf", Lotes.conferencia(l));
        m.put("frete", l.get("frete"));
        m.put("obs", l.get("obs"));
        m.put("temFoto", l.get("temFoto"));
        return m;
    }

    private List<Estatisticas.Ponto> pontos(Connection c, long empresaId) throws SQLException {
        List<Estatisticas.Ponto> pts = new ArrayList<>();
        for (Map<String, Object> p : lotes.entregasDaEmpresa(c, empresaId))
            pts.add(new Estatisticas.Ponto(Instant.parse(Sql.s(p, "entregue_em")), Sql.d(p, "kg"), Sql.l(p, "ong_id"), Sql.s(p, "categoria")));
        return pts;
    }

    /** Cria a fatura do mês corrente se ainda não existir (idempotente). */
    private void garantirFaturaDoMes(Connection c, long empresaId) throws SQLException {
        Empresa e = empresas.buscar(c, empresaId);
        if (e == null || !"ativo".equals(usuarios.status(c, empresaId))) return;
        double preco = PlataformaService.num(plataforma.plano(e.getPlano()), "preco", 0);
        faturas.criarDoMesSeNaoExiste(c, empresaId, e.getPlano(), preco);
    }

    /* ============================ Ações ============================ */

    public record NovoLote(String tipo, String kg, String vol, String cons, String validade, String j1, String j2,
                           String veic, String ong, String obs, boolean termo, byte[] foto) {}

    public Map<String, Object> publicar(Usuario u, NovoLote d) {
        if (!CATEGORIAS.contains(d.tipo())) throw ErroNegocio.invalido("Selecione a categoria.");
        double kg = numero(d.kg(), "Informe o peso.");
        if (kg <= 0 || kg > 50_000) throw ErroNegocio.invalido("Informe um peso entre 0,1 e 50.000 kg.");
        int vol = (int) numero(d.vol(), "Informe a quantidade de volumes.");
        if (vol < 1 || vol > 5_000) throw ErroNegocio.invalido("Informe a quantidade de volumes.");
        if (!List.of("amb", "ref", "cong").contains(d.cons())) throw ErroNegocio.invalido("Selecione o requisito de conservação.");
        if (!CAPACIDADE.containsKey(d.veic())) throw ErroNegocio.invalido("Escolha o veículo.");
        if (kg > CAPACIDADE.get(d.veic())) throw ErroNegocio.invalido("O veículo escolhido não comporta este peso.");
        Instant validade = dataHora(d.validade());
        if (validade == null || !validade.isAfter(Instant.now())) throw ErroNegocio.invalido("Informe uma validade futura.");
        if (validade.isAfter(Instant.now().plusSeconds(60L * 86_400))) throw ErroNegocio.invalido("A validade passa de 60 dias.");
        LocalTime j1 = hora(d.j1()), j2 = hora(d.j2());
        if (j1 != null && j2 != null && !j1.isBefore(j2)) throw ErroNegocio.invalido("O início da janela deve ser antes do fim.");
        if (!d.termo()) throw ErroNegocio.invalido("Aceite o termo de responsabilidade para publicar.");
        String obs = d.obs() == null ? "" : d.obs().trim();
        if (obs.length() > 500) throw ErroNegocio.invalido("As instruções passam de 500 caracteres.");
        if (d.foto() == null || d.foto().length == 0) throw ErroNegocio.invalido("A foto do lote é obrigatória.");
        if (d.foto().length > 5 * 1024 * 1024) throw ErroNegocio.invalido("A foto passa de 5 MB.");
        String tipoFoto = Imagens.detectar(d.foto());
        if (!"image/jpeg".equals(tipoFoto) && !"image/png".equals(tipoFoto)) throw ErroNegocio.invalido("Use uma imagem PNG ou JPG.");

        return Banco.transacao(c -> {
            Map<String, Object> emp = perfil(c, u.getId());
            Long ongId = escolherOng(c, d.ong(), d.tipo(), d.cons(), Sql.d(emp, "lat"), Sql.d(emp, "lon"), null);
            Lote lote = new Lote();
            lote.setEmpresaId(u.getId());
            lote.setOngId(ongId);
            lote.setCategoria(d.tipo());
            lote.setPesoKg(kg);
            lote.setVolumes(vol);
            lote.setConservacao(d.cons());
            lote.setVeiculo(d.veic());
            lote.setValidade(validade);
            lote.setJanelaInicio(j1);
            lote.setJanelaFim(j2);
            lote.setObservacao(obs.isEmpty() ? null : obs);
            lote.setFoto(d.foto());
            lote.setFotoTipo(tipoFoto);
            long id = lotes.inserir(c, lote);
            if (ongId != null)
                notificacoes.notificar(c, ongId, "Nova proposta de doação: lote #" + id + " (" + d.tipo() + ", " + fmt(kg) + " kg) de "
                        + u.getNome() + ". Aceite ou recuse no painel.", "fa-box-open");
            notificacoes.notificar(c, u.getId(), "Lote #" + id + " publicado. " + (ongId == null
                    ? "Nenhuma ONG compatível está disponível agora; a equipa OngSave foi avisada."
                    : "Assim que a ONG aceitar, os motoristas do raio são avisados."), "fa-bullhorn");
            if (ongId == null) notificacoes.notificarAdmins(c, "Lote #" + id + " de " + u.getNome() + " ficou sem ONG compatível.", "fa-triangle-exclamation");
            return Json.obj("id", id, "ongId", ongId);
        });
    }

    /**
     * Escolhe a ONG: a indicada pela empresa, ou a mais próxima que aceite a categoria,
     * tenha câmara fria quando precisa e não tenha recusado este lote.
     */
    private static final UsuarioDAO USUARIOS = new UsuarioDAO();
    private static final OngDAO ONGS = new OngDAO();

    static Long escolherOng(Connection c, String pedido, String categoria, String cons, double lat, double lon, Long loteRecusado) throws SQLException {
        if (pedido != null && !pedido.isBlank() && !"auto".equals(pedido)) {
            long id;
            try { id = Long.parseLong(pedido); } catch (NumberFormatException e) { throw ErroNegocio.invalido("ONG inválida."); }
            if (!USUARIOS.existeAtivo(c, id, Perfil.ONG))
                throw ErroNegocio.invalido("A ONG escolhida não está disponível.");
            return id;
        }
        Long melhor = null;
        double melhorDist = Double.MAX_VALUE;
        for (Map<String, Object> o : ONGS.listarCompativeis(c, categoria, cons, loteRecusado)) {
            double dist = Geo.metros(lat, lon, Sql.d(o, "lat"), Sql.d(o, "lon"));
            if (dist < melhorDist) { melhorDist = dist; melhor = Sql.l(o, "id"); }
        }
        return melhor;
    }

    public void cancelar(Usuario u, long loteId) {
        Banco.transacao(c -> {
            Lote l = lotes.buscarPorId(c, loteId, true);
            if (l == null || l.getEmpresaId() != u.getId()) throw ErroNegocio.naoEncontrado("Lote não encontrado.");
            if (!"aguardando".equals(l.getEstado()))
                throw ErroNegocio.conflito("Só é possível cancelar lotes que ainda aguardam motorista.");
            lotes.cancelar(c, loteId);
            if (l.getOngId() != null)
                notificacoes.notificar(c, l.getOngId(), "A empresa " + u.getNome() + " cancelou o lote #" + loteId + ".", "fa-ban");
            auditoria.registrar(c, u, "Cancelou o lote", "#" + loteId, loteId, null);
            return null;
        });
    }

    /** A empresa confere o que o motorista está a levar e recebe o código que o motorista digita no app. */
    public Map<String, Object> registrarRetirada(Usuario u, long loteId, String kgTxt, String tempTxt, String obs) {
        return Banco.transacao(c -> {
            Lote l = lotes.buscarPorId(c, loteId, true);
            if (l == null || l.getEmpresaId() != u.getId()) throw ErroNegocio.naoEncontrado("Lote não encontrado.");
            if (!"aceito".equals(l.getEstado())) throw ErroNegocio.conflito("O lote não está à espera de retirada.");
            if (l.temRetirada()) throw ErroNegocio.conflito("A retirada deste lote já foi registada.");
            double kg = numero(kgTxt, "Informe o peso retirado.");
            if (kg <= 0 || kg > l.getPesoKg() * 1.5) throw ErroNegocio.invalido("Informe um peso retirado válido.");
            Double temp = null;
            if (!"amb".equals(l.getConservacao())) {
                temp = numero(tempTxt, "Informe a temperatura da carga refrigerada.");
                if (temp < -30 || temp > 30) throw ErroNegocio.invalido("Temperatura fora da faixa esperada.");
            }
            String o = obs == null ? "" : obs.trim();
            if (o.length() > 200) throw ErroNegocio.invalido("A observação passa de 200 caracteres.");
            String codigo = Codigos.gerar(6);
            lotes.registrarRetirada(c, loteId, codigo, kg, temp, o.isEmpty() ? null : o);
            notificacoes.notificar(c, l.getMotoristaId(), "A empresa " + u.getNome() + " registou a retirada do lote #" + loteId
                    + ". Peça o código no balcão e digite-o no app.", "fa-clipboard-check");
            return Json.obj("codigo", codigo, "kg", kg, "temp", temp, "obs", o, "em", System.currentTimeMillis());
        });
    }

    public Map<String, Object> atualizarPerfil(Usuario u, Map<String, String> d) {
        String nome = t(d.get("nome")), email = t(d.get("email")), tel = t(d.get("tel"));
        if (nome.isEmpty() || nome.length() > 160) throw ErroNegocio.invalido("Informe a razão social.");
        if (!email.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) throw ErroNegocio.invalido("Informe um e-mail válido.");
        if (!Documentos.telefoneValido(tel)) throw ErroNegocio.invalido("Informe um telefone com DDD.");
        String descarteTxt = t(d.get("descarte"));
        Double descarte = descarteTxt.isEmpty() ? null : numero(descarteTxt, "Informe um valor válido.");
        if (descarte != null && descarte < 0) throw ErroNegocio.invalido("Informe um valor válido.");
        Endereco end = Endereco.de(d);

        double[] coords = null;
        Map<String, Object> atual = Banco.ler(c -> perfil(c, u.getId()));
        if (end.mudou(atual) || Boolean.TRUE.equals(atual.get("semLocal")))
            coords = geo.localizar(end.rua(), end.num(), end.bairro(), end.cidade(), end.uf(), end.cep());
        final double[] novas = coords;

        return Banco.transacao(c -> {
            if (usuarios.emailEmUsoPorOutro(c, email, u.getId()))
                throw ErroNegocio.conflito("Este e-mail já está em uso por outra conta.");
            usuarios.atualizarContato(c, u.getId(), nome, email, tel);
            Empresa e = new Empresa();
            e.setUsuarioId(u.getId());
            e.setCustoDescarte(descarte);
            e.setCep(end.cep());
            e.setRua(end.rua());
            e.setNumero(end.num());
            e.setComplemento(end.compl());
            e.setBairro(end.bairro());
            e.setCidade(end.cidade());
            e.setUf(end.uf());
            e.setLat(novas == null ? null : novas[0]);
            e.setLon(novas == null ? null : novas[1]);
            empresas.atualizarPerfil(c, e);
            Map<String, Object> r = perfil(c, u.getId());
            r.put("geocodificado", novas != null);
            return r;
        });
    }

    public Map<String, Object> trocarPlano(Usuario u, String plano) {
        if (!List.of("pequena", "media", "grande").contains(plano)) throw ErroNegocio.invalido("Plano desconhecido.");
        Map<String, Object> p = plataforma.plano(plano);
        return Banco.transacao(c -> {
            Empresa atual = empresas.buscar(c, u.getId());
            String antes = atual == null ? null : atual.getPlano();
            if (plano.equals(antes)) return Json.obj("plano", plano);
            empresas.atualizarPlano(c, u.getId(), plano);
            // A fatura aberta do mês acompanha o novo plano.
            faturas.atualizarPlanoDoMes(c, u.getId(), plano, PlataformaService.num(p, "preco", 0));
            auditoria.registrar(c, u, "Alterou o plano", u.getNome(), u.getId(), antes + " → " + plano);
            notificacoes.notificar(c, u.getId(), "Plano alterado para " + p.get("nome") + ".", "fa-file-signature");
            return Json.obj("plano", plano);
        });
    }

    /* ============================ utilitários ============================ */

    static String t(String s) { return s == null ? "" : s.trim(); }

    static double numero(String s, String erro) {
        try { return Double.parseDouble(t(s).replace(',', '.')); }
        catch (NumberFormatException e) { throw ErroNegocio.invalido(erro); }
    }

    private static Instant dataHora(String s) {
        if (s == null || s.isBlank()) return null;
        try { return Instant.parse(s); } catch (DateTimeParseException e) { /* tenta formato local */ }
        try { return LocalDateTime.parse(s).atZone(Estatisticas.FUSO).toInstant(); }
        catch (DateTimeParseException e) { throw ErroNegocio.invalido("Data de validade inválida."); }
    }

    private static LocalTime hora(String s) {
        if (s == null || s.isBlank()) return null;
        try { return LocalTime.parse(s); } catch (DateTimeParseException e) { throw ErroNegocio.invalido("Horário inválido."); }
    }

    static String fmt(double v) {
        return v == Math.rint(v) ? String.valueOf((long) v) : String.format(java.util.Locale.ROOT, "%.1f", v);
    }

    /** Endereço vindo dos formulários de perfil (empresa, ONG, motorista). */
    record Endereco(String cep, String rua, String num, String compl, String bairro, String cidade, String uf) {
        static Endereco de(Map<String, String> d) {
            Endereco e = new Endereco(t(d.get("cep")), t(d.get("rua")), t(d.get("num")), t(d.get("compl")),
                    t(d.get("bairro")), t(d.get("cidade")), t(d.get("uf")).toUpperCase());
            if (!e.cep.matches("\\d{5}-?\\d{3}")) throw ErroNegocio.invalido("Informe o CEP completo.");
            if (e.rua.isEmpty() || e.rua.length() > 160) throw ErroNegocio.invalido("Informe o logradouro.");
            if (e.num.isEmpty() || e.num.length() > 20) throw ErroNegocio.invalido("Informe o número.");
            if (e.bairro.isEmpty()) throw ErroNegocio.invalido("Informe o bairro.");
            if (e.cidade.isEmpty()) throw ErroNegocio.invalido("Informe a cidade.");
            if (!e.uf.matches("[A-Z]{2}")) throw ErroNegocio.invalido("UF inválida.");
            if (e.compl.length() > 80) throw ErroNegocio.invalido("O complemento passa de 80 caracteres.");
            return e;
        }

        boolean mudou(Map<String, Object> atual) {
            return !cep.equals(atual.get("cep")) || !rua.equals(atual.get("rua")) || !num.equals(atual.get("num"))
                    || !bairro.equals(atual.get("bairro")) || !cidade.equals(atual.get("cidade")) || !uf.equals(atual.get("uf"));
        }
    }
}
