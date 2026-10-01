package br.com.ongsave.service;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import br.com.ongsave.dao.LoteDAO;
import br.com.ongsave.dao.MotoristaDAO;
import br.com.ongsave.dao.MovimentoDAO;
import br.com.ongsave.dao.PosicaoDAO;
import br.com.ongsave.dao.UsuarioDAO;
import br.com.ongsave.db.Banco;
import br.com.ongsave.db.Sql;
import br.com.ongsave.model.ErroNegocio;
import br.com.ongsave.model.Lote;
import br.com.ongsave.model.Motorista;
import br.com.ongsave.model.Movimento;
import br.com.ongsave.model.Posicao;
import br.com.ongsave.model.Usuario;
import br.com.ongsave.util.Codigos;
import br.com.ongsave.util.Documentos;
import br.com.ongsave.util.Geo;
import br.com.ongsave.util.Imagens;
import br.com.ongsave.util.Json;
import br.com.ongsave.util.Senhas;

/**
 * Painel do motorista: ofertas no raio, aceite (com bloqueio de concorrência), coleta com código da empresa,
 * rastreio, entrega com token da ONG + foto, carteira e perfil.
 * O servidor revalida tudo o que o front filtra (raio, veículo, validade, proximidade, token).
 */
public class MotoristaService {

    private static final int MAX_TENTATIVAS_TOKEN = 5;
    private static final long FOTO_MAX = 8L * 1024 * 1024;

    private final PlataformaService plataforma;
    private final NotificacaoService notificacoes;
    private final AuditoriaService auditoria;
    private final boolean geoValidar;
    private final int raioValidacaoM;
    /** No modo simulação as posições vêm do simulador; o GPS do aparelho é ignorado. */
    private final boolean simulacao;
    private final MotoristaDAO motoristas = new MotoristaDAO();
    private final LoteDAO lotes = new LoteDAO();
    private final MovimentoDAO movimentosDao = new MovimentoDAO();
    private final PosicaoDAO posicoes = new PosicaoDAO();
    private final UsuarioDAO usuarios = new UsuarioDAO();

    public MotoristaService(PlataformaService plataforma, NotificacaoService notificacoes, AuditoriaService auditoria,
                            boolean geoValidar, int raioValidacaoM, boolean simulacao) {
        this.simulacao = simulacao;
        this.plataforma = plataforma;
        this.notificacoes = notificacoes;
        this.auditoria = auditoria;
        this.geoValidar = geoValidar;
        this.raioValidacaoM = raioValidacaoM;
    }

    /* ============================ Leitura ============================ */

    public Map<String, Object> estado(Usuario u) {
        return Banco.ler(c -> {
            Map<String, Object> r = new LinkedHashMap<>();
            r.put("motorista", perfil(c, u.getId()));
            r.put("historico", historico(c, u.getId()));
            r.put("movimentos", movimentos(c, u.getId()));
            r.put("graficos", graficos(c, u.getId()));
            r.put("notifs", notificacoes.listar(c, u.getId()));
            return r;
        });
    }

    private Map<String, Object> perfil(Connection c, long id) throws SQLException {
        Map<String, Object> m = motoristas.perfilPainel(c, id);
        if (m == null) throw ErroNegocio.naoEncontrado("Motorista não encontrado.");
        return m;
    }

    private List<Map<String, Object>> historico(Connection c, long id) throws SQLException {
        List<Map<String, Object>> r = new ArrayList<>();
        for (Map<String, Object> l : lotes.entreguesDoMotorista(c, id)) {
            String st = movimentosDao.statusDoFrete(c, Sql.l(l, "id"));
            String estado = "estornado".equals(st) ? "Rejeitada" : "retido".equals(st) ? "Em análise" : "Paga";
            double km = Geo.km(Sql.d(l, "empLat"), Sql.d(l, "empLon"), Sql.d(l, "ongLat"), Sql.d(l, "ongLon"));
            r.add(Json.obj("lote", l.get("id"), "dias", l.get("dias"), "empresa", l.get("emp"), "ong", l.get("ong"),
                    "kg", l.get("confKg") != null ? l.get("confKg") : l.get("kg"), "km", Math.round(km * 10) / 10.0,
                    "frete", "Rejeitada".equals(estado) ? 0 : l.get("frete"), "estado", estado));
        }
        return r;
    }

    private List<Map<String, Object>> movimentos(Connection c, long id) throws SQLException {
        List<Map<String, Object>> r = new ArrayList<>();
        for (Map<String, Object> m : movimentosDao.extrato(c, id)) {
            String st = Sql.s(m, "status");
            boolean estornado = "estornado".equals(st);
            r.add(Json.obj("desc", Sql.s(m, "descricao") + (estornado ? " · estornado" : "solicitado".equals(st) ? " · em processamento" : ""),
                    "dias", m.get("dias"), "v", estornado ? 0 : m.get("valor"), "retido", "retido".equals(st)));
        }
        return r;
    }

    private Map<String, Object> graficos(Connection c, long id) throws SQLException {
        double[] sem = new double[4];
        for (Map<String, Object> m : movimentosDao.fretesUltimas4Semanas(c, id)) {
            int s = (int) Sql.l(m, "s");
            if (s >= 0 && s < 4) sem[3 - s] += Sql.d(m, "valor");
        }
        List<Long> ganhos = new ArrayList<>();
        for (double v : sem) ganhos.add(Math.round(v));
        List<String> labels = new ArrayList<>();
        List<Long> dados = new ArrayList<>();
        for (Map<String, Object> m : lotes.kgPorCategoriaDoMotorista(c, id)) {
            labels.add(Estatisticas.curta(Sql.s(m, "categoria")));
            dados.add(Math.round(Sql.d(m, "kg")));
        }
        return Json.obj("ganhos", Json.obj("labels", List.of("Sem 1", "Sem 2", "Sem 3", "Sem 4"), "data", ganhos),
                "cargas", Json.obj("labels", labels, "data", dados));
    }

    /* ============================ Ofertas e entrega ============================ */

    /** Lotes publicados, já aceites pela ONG, válidos, compatíveis com o veículo e dentro do raio da plataforma. */
    public List<Map<String, Object>> ofertas(Usuario u, double lat, double lon) {
        if (!Geo.coordenadaValida(lat, lon)) throw ErroNegocio.invalido("Posição inválida.");
        double raioKm = plataforma.raioKm();
        return Banco.ler(c -> {
            Motorista m = motoristas.buscar(c, u.getId(), false);
            if (m == null) throw ErroNegocio.naoEncontrado("Motorista não encontrado.");
            int rank = Lotes.rankMotorista(m.getVeiculo());
            List<Map<String, Object>> r = new ArrayList<>();
            for (Map<String, Object> l : lotes.ofertas(c)) {
                if (Lotes.rankLote(Sql.s(l, "veicKey")) > rank) continue;
                double trajeto = Geo.km(lat, lon, Sql.d(l, "empLat"), Sql.d(l, "empLon"))
                        + Geo.km(Sql.d(l, "empLat"), Sql.d(l, "empLon"), Sql.d(l, "ongLat"), Sql.d(l, "ongLon"));
                if (geoValidar && trajeto > raioKm) continue;
                r.add(oferta(l, plataforma.freteDoLote(Sql.s(l, "veicKey"))));
            }
            return r;
        });
    }

    private static Map<String, Object> oferta(Map<String, Object> l, double frete) {
        return Json.obj("id", l.get("id"), "empresa", l.get("emp"), "ong", l.get("ong"),
                "pickupLat", l.get("empLat"), "pickupLon", l.get("empLon"), "destLat", l.get("ongLat"), "destLon", l.get("ongLon"),
                "pesoKg", l.get("kg"), "produto", Estatisticas.curta(Sql.s(l, "tipo")).toLowerCase(), "volumes", l.get("volumes"),
                "veiculo", Lotes.veiculoMotorista(Sql.s(l, "veicKey")), "cons", l.get("cons"), "obs", l.get("obs"),
                "validade", l.get("validade"), "frete", l.get("frete") != null ? l.get("frete") : frete);
    }

    /** Entrega em andamento ({@code fase}: coleta | transito) ou null. */
    public Map<String, Object> entregaAtiva(Usuario u) {
        return Banco.ler(c -> {
            Map<String, Object> l = lotes.detalheAtivoDoMotorista(c, u.getId());
            if (l == null) return null;
            Map<String, Object> o = oferta(l, 0);
            o.put("fase", "aceito".equals(l.get("estado")) ? "coleta" : "transito");
            o.put("retiradaRegistada", l.get("retCodigo") != null);
            o.put("posLat", l.get("posLat"));
            o.put("posLon", l.get("posLon"));
            return o;
        });
    }

    public Map<String, Object> aceitar(Usuario u, long loteId) {
        return Banco.transacao(c -> {
            Motorista m = motoristas.buscar(c, u.getId(), false);
            if (m == null) throw ErroNegocio.naoEncontrado("Motorista não encontrado.");
            if (!m.temConsentimento())
                throw ErroNegocio.invalido("Dê o consentimento de localização para aceitar entregas.");
            if (m.isCnhVencida())
                throw ErroNegocio.invalido("A sua CNH está vencida. Atualize a validade no perfil.");
            if (lotes.motoristaTemEntregaAtiva(c, u.getId()))
                throw ErroNegocio.conflito("Você já tem uma entrega em andamento. Conclua-a antes.");

            Lote l = lotes.buscarPorId(c, loteId, false);
            if (l == null) throw ErroNegocio.naoEncontrado("Entrega não encontrada.");
            if (Lotes.rankLote(l.getVeiculo()) > Lotes.rankMotorista(m.getVeiculo()))
                throw ErroNegocio.invalido("O seu veículo não comporta esta carga.");

            double frete = plataforma.freteDoLote(l.getVeiculo());
            // Aceite atómico: só um motorista consegue (os outros recebem 409).
            if (lotes.atribuirMotorista(c, loteId, u.getId(), frete) == 0) throw ErroNegocio.conflito("Outro motorista aceitou primeiro.");

            Lote info = lotes.buscarPorId(c, loteId, false);
            notificacoes.notificar(c, info.getEmpresaId(), "O motorista " + u.getNome() + " (" + m.getVeiculo() + " · "
                    + (m.getPlaca() == null ? "" : m.getPlaca()) + ") aceitou o lote #" + loteId + ". Registe a retirada quando ele chegar.", "fa-user-check");
            notificacoes.notificar(c, info.getOngId(), "Um motorista aceitou levar o lote #" + loteId + ". Ele está a caminho da coleta.", "fa-truck-fast");
            return Json.obj("id", loteId, "frete", frete);
        });
    }

    public void coletar(Usuario u, double lat, double lon, String codigo) {
        Banco.transacao(c -> {
            Map<String, Object> l = ativa(c, u.getId(), "aceito");
            if (l.get("retCodigo") == null)
                throw ErroNegocio.invalido("A empresa ainda não registou a retirada. Peça para registarem o peso no painel.");
            if (geoValidar && (!Geo.coordenadaValida(lat, lon)
                    || Geo.metros(lat, lon, Sql.d(l, "empLat"), Sql.d(l, "empLon")) > raioValidacaoM))
                throw ErroNegocio.invalido("Aproxime-se a menos de " + raioValidacaoM + " m da empresa para confirmar a coleta.");
            if (!Senhas.iguais(Codigos.normalizar(codigo), Sql.s(l, "retCodigo")))
                throw ErroNegocio.invalido("Código de retirada inválido. Confira com a empresa.");
            long id = Sql.l(l, "id");
            lotes.marcarColetado(c, id);
            if (Geo.coordenadaValida(lat, lon)) posicoes.inserir(c, posicao(id, u.getId(), lat, lon, null));
            notificacoes.notificar(c, Sql.l(l, "empresaId"), "Lote #" + id + " retirado e a caminho da " + l.get("ong") + ".", "fa-truck-fast");
            notificacoes.notificar(c, Sql.l(l, "ongId"), "O lote #" + id + " (" + l.get("emp") + ") saiu para entrega. Prepare o recebimento.", "fa-truck-fast");
            return null;
        });
    }

    public void registrarPosicao(Usuario u, double lat, double lon, Double precisao) {
        if (!Geo.coordenadaValida(lat, lon)) throw ErroNegocio.invalido("Posição inválida.");
        if (simulacao) return;
        Banco.transacao(c -> {
            Long loteId = lotes.idAtivoDoMotorista(c, u.getId());
            if (loteId == null) return null;      // sem entrega ativa: não rastreia (LGPD)
            if (!posicoes.existeRecente(c, loteId, 3))
                posicoes.inserir(c, posicao(loteId, u.getId(), lat, lon, precisao == null ? null : precisao.floatValue()));
            return null;
        });
    }

    private static Posicao posicao(long loteId, long motoristaId, double lat, double lon, Float precisao) {
        Posicao p = new Posicao();
        p.setLoteId(loteId);
        p.setMotoristaId(motoristaId);
        p.setLat(lat);
        p.setLon(lon);
        p.setPrecisaoM(precisao);
        return p;
    }

    public void finalizar(Usuario u, String token, byte[] foto, double lat, double lon, String capturadaEm) {
        if (foto == null || foto.length == 0) throw ErroNegocio.invalido("Tire a foto da descarga antes de validar.");
        if (foto.length > FOTO_MAX) throw ErroNegocio.invalido("A foto passa de 8 MB. Tire outra.");
        String tipo = Imagens.detectar(foto);
        if (!Imagens.ehImagem(tipo)) throw ErroNegocio.invalido("O ficheiro enviado não é uma foto válida.");
        Instant capturada = null;
        try { if (capturadaEm != null && !capturadaEm.isBlank()) capturada = Instant.parse(capturadaEm); }
        catch (DateTimeParseException e) { /* ignorado: o servidor usa a hora de receção */ }
        if (capturada != null && (capturada.isAfter(Instant.now().plusSeconds(120)) || capturada.isBefore(Instant.now().minus(Duration.ofHours(2)))))
            capturada = null;
        final Instant quando = capturada;

        // A transação devolve a mensagem de erro do token (em vez de lançar) para que a contagem
        // de tentativas falhadas seja gravada no mesmo commit.
        String erroToken = Banco.transacao(c -> {
            Map<String, Object> l = ativa(c, u.getId(), "transito");
            long id = Sql.l(l, "id");
            double dist = Geo.coordenadaValida(lat, lon) ? Geo.metros(lat, lon, Sql.d(l, "ongLat"), Sql.d(l, "ongLon")) : Double.NaN;
            if (geoValidar && (Double.isNaN(dist) || dist > raioValidacaoM))
                throw ErroNegocio.invalido("Aproxime-se a menos de " + raioValidacaoM + " m da ONG para validar a entrega.");

            Lote t = lotes.buscarPorId(c, id, true);
            if (!t.isTokenValido())
                return "Token inválido ou expirado. Peça à ONG para gerar um novo.";
            if (!Senhas.iguais(Codigos.normalizar(token), t.getTokenCodigo())) {
                int tent = t.getTokenTentativas() + 1;
                if (tent >= MAX_TENTATIVAS_TOKEN) {
                    lotes.expirarToken(c, id);
                    return "Token errado demasiadas vezes. Peça à ONG para gerar um novo.";
                }
                lotes.registrarTentativaToken(c, id, tent);
                return "Token inválido ou expirado.";
            }

            boolean comGps = Geo.coordenadaValida(lat, lon);
            lotes.registrarEntrega(c, id, foto, tipo, comGps ? lat : null, comGps ? lon : null,
                    Double.isNaN(dist) ? null : (int) Math.round(dist), quando == null ? Instant.now() : quando);
            // O frete entra retido até a ONG conferir o recebimento.
            Movimento mv = new Movimento();
            mv.setMotoristaId(u.getId());
            mv.setLoteId(id);
            mv.setTipo("frete");
            mv.setDescricao("Frete #" + id + " · " + l.get("emp") + " → " + l.get("ong"));
            mv.setValor(t.getFrete() == null ? 0 : t.getFrete());
            mv.setStatus("retido");
            movimentosDao.inserir(c, mv);
            notificacoes.notificar(c, Sql.l(l, "ongId"), "Entrega do lote #" + id + " validada: token e foto do motorista " + u.getNome()
                    + " conferem. Faça a conferência do recebimento.", "fa-circle-check");
            notificacoes.notificar(c, Sql.l(l, "empresaId"), "Lote #" + id + " entregue na " + l.get("ong") + ". Token validado.", "fa-circle-check");
            return null;
        });
        if (erroToken != null) throw ErroNegocio.invalido(erroToken);
    }

    private Map<String, Object> ativa(Connection c, long motoristaId, String estadoEsperado) throws SQLException {
        Map<String, Object> l = lotes.detalheAtivoDoMotorista(c, motoristaId);
        if (l == null) throw ErroNegocio.naoEncontrado("Você não tem entrega em andamento.");
        if (!estadoEsperado.equals(l.get("estado")))
            throw ErroNegocio.conflito("aceito".equals(estadoEsperado) ? "A coleta deste lote já foi confirmada." : "Confirme a coleta antes de entregar.");
        return l;
    }

    /* ============================ Perfil e carteira ============================ */

    public Map<String, Object> atualizarPerfil(Usuario u, Map<String, String> d) {
        String nome = EmpresaService.t(d.get("nome")), email = EmpresaService.t(d.get("email")), tel = EmpresaService.t(d.get("tel"));
        String veiculo = EmpresaService.t(d.get("veiculo"));
        String placa = Documentos.placa(d.get("placa"));
        String cnh = Documentos.digitos(d.get("cnh"));
        String cat = EmpresaService.t(d.get("cat"));
        if (nome.length() < 3) throw ErroNegocio.invalido("Informe o nome completo.");
        if (!email.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) throw ErroNegocio.invalido("Informe um e-mail válido.");
        if (!Documentos.telefoneValido(tel)) throw ErroNegocio.invalido("Informe um telefone com DDD.");
        if (!List.of("Carro Económico", "Camionete", "Van", "Caminhão").contains(veiculo)) throw ErroNegocio.invalido("Escolha o tipo de veículo.");
        if (placa == null) throw ErroNegocio.invalido("Informe a placa no formato ABC1D23 ou ABC1234.");
        if (cnh.length() != 11) throw ErroNegocio.invalido("O número da CNH tem 11 dígitos.");
        if (!List.of("B", "C", "D", "E").contains(cat)) throw ErroNegocio.invalido("Categoria da CNH inválida.");
        if ("Caminhão".equals(veiculo) && "B".equals(cat)) throw ErroNegocio.invalido("Caminhão exige CNH categoria C ou superior.");
        LocalDate validade;
        try { validade = LocalDate.parse(EmpresaService.t(d.get("cnhValidade"))); }
        catch (DateTimeParseException e) { throw ErroNegocio.invalido("Informe a validade da CNH."); }
        String modelo = EmpresaService.t(d.get("modelo"));
        if (modelo.length() > 80) throw ErroNegocio.invalido("O modelo passa de 80 caracteres.");
        EmpresaService.Endereco end = EmpresaService.Endereco.de(d);

        return Banco.transacao(c -> {
            if (usuarios.emailEmUsoPorOutro(c, email, u.getId()))
                throw ErroNegocio.conflito("Este e-mail já está em uso por outra conta.");
            usuarios.atualizarContato(c, u.getId(), nome, email, tel);
            Motorista m = new Motorista();
            m.setUsuarioId(u.getId());
            m.setVeiculo(veiculo);
            m.setPlaca(placa);
            m.setModelo(modelo);
            m.setCnh(cnh);
            m.setCnhCategoria(cat);
            m.setCnhValidade(validade);
            m.setCep(end.cep());
            m.setRua(end.rua());
            m.setNumero(end.num());
            m.setBairro(end.bairro());
            m.setCidade(end.cidade());
            m.setUf(end.uf());
            motoristas.atualizarPerfil(c, m);
            return perfil(c, u.getId());
        });
    }

    public Map<String, Object> consentimento(Usuario u, boolean conceder) {
        return Banco.transacao(c -> {
            if (!conceder && lotes.motoristaTemEntregaAtiva(c, u.getId()))
                throw ErroNegocio.conflito("Conclua a entrega em andamento antes de revogar o consentimento.");
            motoristas.definirConsentimento(c, u.getId(), conceder);
            auditoria.registrar(c, u, conceder ? "Deu consentimento de localização" : "Revogou consentimento de localização", u.getNome(), u.getId(), "LGPD");
            Motorista m = motoristas.buscar(c, u.getId(), false);
            Instant em = m == null ? null : m.getConsentimentoEm();
            return Json.obj("consentimento", em == null ? null : em.toString());
        });
    }

    public Map<String, Object> sacar(Usuario u, String valorTxt) {
        double v = Math.round(EmpresaService.numero(valorTxt, "Informe um valor maior que zero.") * 100) / 100.0;
        if (!(v > 0)) throw ErroNegocio.invalido("Informe um valor maior que zero.");
        return Banco.transacao(c -> {
            motoristas.buscar(c, u.getId(), true); // bloqueia a linha do motorista: serializa saques simultâneos
            double saldo = movimentosDao.saldoDisponivel(c, u.getId());
            if (v > saldo + 0.001) throw ErroNegocio.invalido("O valor passa do saldo disponível.");
            Movimento mv = new Movimento();
            mv.setMotoristaId(u.getId());
            mv.setTipo("saque");
            mv.setDescricao("Saque via Pix");
            mv.setValor(-v);
            mv.setStatus("solicitado");
            movimentosDao.inserir(c, mv);
            auditoria.registrar(c, u, "Solicitou saque", u.getNome(), u.getId(), "R$ " + String.format(java.util.Locale.ROOT, "%.2f", v));
            return Json.obj("saldo", Math.round((saldo - v) * 100) / 100.0);
        });
    }

    public void contestar(Usuario u, String texto) {
        String t = EmpresaService.t(texto);
        if (t.length() < 10) throw ErroNegocio.invalido("Descreva a decisão que quer contestar (mínimo 10 caracteres).");
        if (t.length() > 600) throw ErroNegocio.invalido("O texto passa de 600 caracteres.");
        Banco.transacao(c -> {
            auditoria.registrar(c, u, "Contestou uma decisão", u.getNome(), u.getId(), t);
            notificacoes.notificarAdmins(c, "Contestação de " + u.getNome() + ": " + t, "fa-scale-balanced");
            return null;
        });
    }
}
