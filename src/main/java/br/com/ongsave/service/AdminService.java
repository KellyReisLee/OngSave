package br.com.ongsave.service;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import br.com.ongsave.dao.AuditoriaDAO;
import br.com.ongsave.dao.DocumentoDAO;
import br.com.ongsave.dao.EmpresaDAO;
import br.com.ongsave.dao.LoteDAO;
import br.com.ongsave.dao.MovimentoDAO;
import br.com.ongsave.dao.NotaAdminDAO;
import br.com.ongsave.dao.OcorrenciaDAO;
import br.com.ongsave.dao.RelatorioDAO;
import br.com.ongsave.dao.UsuarioDAO;
import br.com.ongsave.db.Banco;
import br.com.ongsave.db.Sql;
import br.com.ongsave.model.Conta;
import br.com.ongsave.model.Documento;
import br.com.ongsave.model.ErroNegocio;
import br.com.ongsave.model.Lote;
import br.com.ongsave.model.Movimento;
import br.com.ongsave.model.NotaAdmin;
import br.com.ongsave.model.Ocorrencia;
import br.com.ongsave.model.Perfil;
import br.com.ongsave.model.Usuario;
import br.com.ongsave.util.Json;

/**
 * Painel do administrador: moderação de contas e documentos, risco, ocorrências (com decisão sobre o frete retido),
 * operações ao vivo, auditoria e finanças.
 */
public class AdminService {

    private static final List<String> STATUS = List.of("pendente", "ativo", "suspenso", "bloqueado", "rejeitado");

    private final PlataformaService plataforma;
    private final NotificacaoService notificacoes;
    private final AuditoriaService auditoria;
    private final UsuarioDAO usuariosDao = new UsuarioDAO();
    private final DocumentoDAO documentos = new DocumentoDAO();
    private final NotaAdminDAO notas = new NotaAdminDAO();
    private final LoteDAO lotes = new LoteDAO();
    private final MovimentoDAO movimentos = new MovimentoDAO();
    private final OcorrenciaDAO ocorrenciasDao = new OcorrenciaDAO();
    private final AuditoriaDAO auditoriaDao = new AuditoriaDAO();
    private final EmpresaDAO empresas = new EmpresaDAO();
    private final RelatorioDAO relatorios = new RelatorioDAO();

    public AdminService(PlataformaService plataforma, NotificacaoService notificacoes, AuditoriaService auditoria) {
        this.plataforma = plataforma;
        this.notificacoes = notificacoes;
        this.auditoria = auditoria;
    }

    /* ============================ Leitura ============================ */

    public Map<String, Object> estado(Usuario admin) {
        return Banco.ler(c -> {
            Map<String, Object> r = new LinkedHashMap<>();
            r.put("usuarios", usuarios(c));
            r.put("pares", pares(c));
            r.put("log", auditoriaDao.listarRecentes(c, 300));
            r.put("ocorrencias", ocorrencias(c));
            r.put("ops", ops(c));
            r.put("feed", feed(c));
            r.put("fin", financas(c));
            r.put("entregasSemana", entregasSemana(c));
            r.put("notifs", notificacoes.listar(c, admin.getId()));
            return r;
        });
    }

    private List<Map<String, Object>> usuarios(Connection c) throws SQLException {
        List<Map<String, Object>> base = usuariosDao.listarParaAdministracao(c);
        Map<Long, Map<String, Object>> stats = relatorios.estatisticasPorUsuario(c);
        Map<Long, Long> ocorr = relatorios.ocorrenciasPorUsuario30Dias(c);
        Map<Long, Long> recusas = relatorios.recusasPorOng90Dias(c);
        Map<Long, Long> aceites = relatorios.aceitesPorOng90Dias(c);
        Map<Long, Long> usoMes = relatorios.lotesDoMesPorEmpresa(c);

        Map<Long, List<Map<String, Object>>> docs = new HashMap<>();
        for (Map<String, Object> d : documentos.listarTodos(c)) {
            long uid = Sql.l(d, "usuario_id");
            d.remove("usuario_id");
            docs.computeIfAbsent(uid, k -> new ArrayList<>()).add(d);
        }
        Map<Long, List<Map<String, Object>>> notasPorUsuario = new HashMap<>();
        for (Map<String, Object> n : notas.listarRecentes(c, 3000)) {
            long uid = Sql.l(n, "usuario_id");
            n.remove("usuario_id");
            notasPorUsuario.computeIfAbsent(uid, k -> new ArrayList<>()).add(n);
        }

        Map<String, Object> planos = PlataformaService.sub(plataforma.config(), "planos");
        List<Map<String, Object>> r = new ArrayList<>(base.size());
        for (Map<String, Object> u : base) {
            long id = Sql.l(u, "id");
            String tipo = Sql.s(u, "tipo");
            Map<String, Object> s = stats.getOrDefault(id, Map.of());
            long entregas = s.isEmpty() ? 0 : Sql.l(s, "entregas");
            long rej = 0;
            if ("motorista".equals(tipo) && entregas > 0) rej = Math.round(100.0 * Sql.l(s, "retidos") / entregas);
            if ("ong".equals(tipo)) {
                long rc = recusas.getOrDefault(id, 0L), ac = aceites.getOrDefault(id, 0L);
                rej = rc + ac == 0 ? 0 : Math.round(100.0 * rc / (rc + ac));
            }
            long extras = 0;
            if ("empresa".equals(tipo) && u.get("plano") != null) {
                long franquia = (long) PlataformaService.num(PlataformaService.sub(planos, Sql.s(u, "plano")), "franquia", 0);
                extras = Math.max(0, usoMes.getOrDefault(id, 0L) - franquia);
            }
            u.put("st", Json.obj("entregas", entregas, "kg", s.isEmpty() || s.get("kg") == null ? 0 : s.get("kg"), "rej", rej,
                    "ocorr", ocorr.getOrDefault(id, 0L), "aval", 0, "extras", extras));
            u.put("docs", docs.getOrDefault(id, List.of()));
            u.put("notas", notasPorUsuario.getOrDefault(id, List.of()));
            // Remove campos que não se aplicam ao perfil (o front usa a presença deles).
            if (!"empresa".equals(tipo)) u.remove("plano");
            if (!"motorista".equals(tipo)) { u.remove("veiculo"); u.remove("placa"); u.remove("cnhCat"); u.remove("cnhDias"); }
            if (!"ong".equals(tipo)) { u.remove("familias"); u.remove("capKg"); u.remove("alvaraDias"); }
            r.add(u);
        }
        return r;
    }

    /** Concentração motorista × ONG nos últimos 90 dias (sinal clássico de conluio para liberar pagamento). */
    private List<Map<String, Object>> pares(Connection c) throws SQLException {
        return relatorios.paresMotoristaOng(c);
    }

    private List<Map<String, Object>> ocorrencias(Connection c) throws SQLException {
        return ocorrenciasDao.listarParaAdministracao(c);
    }

    private List<Map<String, Object>> ops(Connection c) throws SQLException {
        List<Map<String, Object>> r = new ArrayList<>();
        for (Map<String, Object> l : lotes.detalhesEmOperacao(c)) {
            String est = Sql.s(l, "estado");
            String fase;
            if ("aguardando".equals(est)) fase = Boolean.TRUE.equals(l.get("ongAceitou")) ? "aguardando" : "proposta";
            else if ("aceito".equals(est)) fase = "coleta";
            else {
                double m = Lotes.metrosAteOng(l);
                fase = !Double.isNaN(m) && m <= 200 ? "porta" : "caminho";
            }
            r.add(Json.obj("id", l.get("id"), "emp", l.get("emp"), "ong", l.get("ong") == null ? "Sem ONG" : l.get("ong"),
                    "mot", l.get("mot") == null ? "—" : l.get("mot"), "prog", Lotes.progresso(l), "fase", fase,
                    "empLat", l.get("empLat"), "empLon", l.get("empLon"), "ongLat", l.get("ongLat"), "ongLon", l.get("ongLon"),
                    "posLat", l.get("posLat"), "posLon", l.get("posLon"), "placa", l.get("placa")));
        }
        return r;
    }

    private List<Map<String, Object>> feed(Connection c) throws SQLException {
        return relatorios.atividadeRecente(c);
    }

    private List<Long> entregasSemana(Connection c) throws SQLException {
        long[] s = new long[6];
        for (Map<String, Object> l : relatorios.entregasPorSemana(c)) {
            int w = (int) Sql.l(l, "w");
            if (w >= 0 && w < 6) s[5 - w] = Sql.l(l, "n");
        }
        List<Long> r = new ArrayList<>();
        for (long v : s) r.add(v);
        return r;
    }

    /** Receita (faturas) e fretes dos últimos 6 meses; o mês corrente usa a receita contratada (planos + extras). */
    private Map<String, Object> financas(Connection c) throws SQLException {
        YearMonth agora = YearMonth.now(Estatisticas.FUSO);
        Map<String, Double> rec = new HashMap<>(), fre = new HashMap<>();
        for (Map<String, Object> f : relatorios.receitaPorMes(c))
            rec.put(Sql.s(f, "m"), Sql.d(f, "v"));
        for (Map<String, Object> f : relatorios.fretesPorMes(c))
            fre.put(Sql.s(f, "m"), Sql.d(f, "v"));

        Map<String, Object> cfg = plataforma.config();
        Map<String, Object> planos = PlataformaService.sub(cfg, "planos");
        double contratado = 0;
        for (Map<String, Object> e : empresas.listarAtivasComUsoDoMes(c)) {
            Map<String, Object> p = PlataformaService.sub(planos, Sql.s(e, "plano"));
            contratado += PlataformaService.num(p, "preco", 0)
                    + Math.max(0, Sql.l(e, "uso") - PlataformaService.num(p, "franquia", 0)) * PlataformaService.num(p, "taxaExtra", 0);
        }

        List<Long> receita = new ArrayList<>(), fretes = new ArrayList<>();
        for (int i = 5; i >= 0; i--) {
            String m = agora.minusMonths(i).toString();
            receita.add(Math.round(i == 0 ? contratado : rec.getOrDefault(m, 0.0)));
            fretes.add(Math.round(fre.getOrDefault(m, 0.0)));
        }
        List<Map<String, Object>> movs = relatorios.movimentosRecentes(c);
        List<Map<String, Object>> faturas = relatorios.faturasRecentes(c);
        return Json.obj("receita", receita, "fretes", fretes, "movimentos", movs, "faturas", faturas);
    }

    /* ============================ Ações sobre contas ============================ */

    public Map<String, Object> mudarStatus(Usuario admin, long id, String novo, String motivo, String obs) {
        if (!STATUS.contains(novo)) throw ErroNegocio.invalido("Situação desconhecida.");
        String m = EmpresaService.t(motivo), o = EmpresaService.t(obs);
        if (List.of("suspenso", "bloqueado", "rejeitado").contains(novo) && o.isEmpty())
            throw ErroNegocio.invalido("Descreva o motivo.");
        if (o.length() > 300) throw ErroNegocio.invalido("A observação passa de 300 caracteres.");
        return Banco.transacao(c -> {
            Conta u = usuariosDao.buscarPorId(c, id, true);
            if (u == null) throw ErroNegocio.naoEncontrado("Utilizador não encontrado.");
            if (u.getPerfil() == Perfil.ADMIN) throw ErroNegocio.proibido("Contas de administrador não são geridas por este painel.");
            String antes = u.getStatus();
            if (antes.equals(novo)) return Json.obj("status", novo);
            if ("ativo".equals(novo) && "pendente".equals(antes) && documentos.temPendentes(c, id))
                throw ErroNegocio.conflito("Aprove todos os documentos antes de aprovar o cadastro.");
            if (!"ativo".equals(novo) && lotes.motoristaTemEntregaAtiva(c, id))
                throw ErroNegocio.conflito("O motorista tem uma entrega em andamento. Aguarde a conclusão ou trate-a como ocorrência.");

            usuariosDao.atualizarStatus(c, id, novo);
            String acao = switch (novo) {
                case "ativo" -> "pendente".equals(antes) ? "Aprovou o cadastro" : "Reativou a conta";
                case "suspenso" -> "Suspendeu a conta";
                case "bloqueado" -> "Bloqueou a conta";
                case "rejeitado" -> "Rejeitou o cadastro";
                default -> "Reabriu a análise do cadastro";
            };
            String extra = m + (o.isEmpty() ? "" : (m.isEmpty() ? "" : ": ") + o);
            auditoria.registrar(c, admin, acao, u.getNome(), id, extra.isEmpty() ? null : extra);
            String aviso = switch (novo) {
                case "ativo" -> "pendente".equals(antes) ? "Cadastro aprovado pela administração da plataforma. Bem-vindo à OngSave!" : "A sua conta foi reativada.";
                case "suspenso" -> "A sua conta foi suspensa. Motivo: " + extra + ". Pode contestar a decisão com o suporte.";
                case "bloqueado" -> "A sua conta foi bloqueada. Motivo: " + extra + ".";
                case "rejeitado" -> "O seu cadastro não foi aprovado. Motivo: " + extra + ".";
                default -> "O seu cadastro voltou para análise.";
            };
            notificacoes.notificar(c, id, aviso, "ativo".equals(novo) ? "fa-circle-check" : "fa-triangle-exclamation");
            // Lotes ainda sem motorista de uma ONG que deixa de estar ativa voltam a procurar ONG.
            if (!"ativo".equals(novo) && u.getPerfil() == Perfil.ONG)
                lotes.desvincularOngDosAguardando(c, id);
            return Json.obj("status", novo);
        });
    }

    public Map<String, Object> documento(Usuario admin, long docId, String status, String motivo, String obs) {
        if (!List.of("ok", "rejeitado", "analise").contains(status)) throw ErroNegocio.invalido("Situação de documento inválida.");
        String m = EmpresaService.t(motivo), o = EmpresaService.t(obs);
        if ("rejeitado".equals(status) && o.isEmpty()) throw ErroNegocio.invalido("Descreva o motivo.");
        return Banco.transacao(c -> {
            Documento d = documentos.buscar(c, docId);
            if (d == null) throw ErroNegocio.naoEncontrado("Documento não encontrado.");
            String dono = usuariosDao.nome(c, d.getUsuarioId());
            documentos.avaliar(c, docId, status, "rejeitado".equals(status) ? AuditoriaService.corta(m.isEmpty() ? o : m, 200) : null);
            auditoria.registrar(c, admin, "ok".equals(status) ? "Aprovou documento" : "rejeitado".equals(status) ? "Rejeitou documento" : "Reabriu documento",
                    dono, d.getUsuarioId(), d.getNome() + ("rejeitado".equals(status) ? " · " + m + ": " + o : ""));
            notificacoes.notificar(c, d.getUsuarioId(), "ok".equals(status) ? "Documento \"" + d.getNome() + "\" aprovado."
                    : "Documento \"" + d.getNome() + "\" rejeitado (" + (m.isEmpty() ? o : m) + "). Envie um novo arquivo no perfil.", "fa-file-circle-check");
            return Json.obj("status", status, "motivo", "rejeitado".equals(status) ? (m.isEmpty() ? o : m) : null);
        });
    }

    public Map<String, Object> nota(Usuario admin, long usuarioId, String texto) {
        String t = EmpresaService.t(texto);
        if (t.isEmpty()) throw ErroNegocio.invalido("Escreva a nota.");
        if (t.length() > 1000) throw ErroNegocio.invalido("A nota passa de 1000 caracteres.");
        return Banco.transacao(c -> {
            if (!usuariosDao.existe(c, usuarioId)) throw ErroNegocio.naoEncontrado("Utilizador não encontrado.");
            NotaAdmin n = new NotaAdmin();
            n.setUsuarioId(usuarioId);
            n.setAutorId(admin.getId());
            n.setTexto(t);
            notas.inserir(c, n);
            return Json.obj("t", System.currentTimeMillis(), "txt", t, "autor", admin.getNome());
        });
    }

    public void solicitarDocumento(Usuario admin, long usuarioId, String documento, String obs) {
        String d = EmpresaService.t(documento), o = EmpresaService.t(obs);
        if (d.isEmpty()) throw ErroNegocio.invalido("Escolha o documento.");
        if (o.length() > 300) throw ErroNegocio.invalido("A observação passa de 300 caracteres.");
        Banco.transacao(c -> {
            String nome = usuariosDao.nome(c, usuarioId);
            if (nome == null) throw ErroNegocio.naoEncontrado("Utilizador não encontrado.");
            // Se o documento já existe na lista do utilizador, volta a "rejeitado" para ele enviar um novo arquivo.
            documentos.solicitarNovoEnvio(c, usuarioId, d);
            notificacoes.notificar(c, usuarioId, "A administração solicitou o documento \"" + d + "\"" + (o.isEmpty() ? "" : ": " + o)
                    + ". Envie-o pelo seu perfil.", "fa-file-circle-exclamation");
            auditoria.registrar(c, admin, "Solicitou documento", nome, usuarioId, d + (o.isEmpty() ? "" : ": " + o));
            return null;
        });
    }

    /** Ficheiro de um documento enviado (para o admin conferir). */
    public Map<String, Object> arquivoDocumento(long docId) {
        return Banco.ler(c -> {
            Documento d = documentos.buscarComArquivo(c, docId);
            if (d == null) throw ErroNegocio.naoEncontrado("Arquivo não encontrado.");
            Map<String, Object> r = new HashMap<>();
            r.put("arquivo_nome", d.getArquivoNome());
            r.put("content_type", d.getContentType());
            r.put("bytes", d.getConteudo());
            return r;
        });
    }

    /* ============================ Ocorrências ============================ */

    public void analisarOcorrencia(Usuario admin, long id) {
        Banco.transacao(c -> {
            if (ocorrenciasDao.iniciarAnalise(c, id) == 0) throw ErroNegocio.conflito("A ocorrência já está em análise ou resolvida.");
            auditoria.registrar(c, admin, "Iniciou análise da ocorrência", "#" + id, null, null);
            return null;
        });
    }

    /**
     * Resolve a ocorrência e decide o frete retido do lote:
     * liberar (sem irregularidade) · ajustar (proporcional ao peso conferido) · estornar (sanção).
     */
    public void resolverOcorrencia(Usuario admin, long id, String motivo, String obs) {
        String m = EmpresaService.t(motivo), o = EmpresaService.t(obs);
        if (o.isEmpty()) throw ErroNegocio.invalido("Descreva a decisão.");
        if (o.length() > 300) throw ErroNegocio.invalido("A observação passa de 300 caracteres.");
        String decisao = m.startsWith("Frete ajustado") ? "ajustar" : m.startsWith("Conta sancionada") || m.startsWith("Frete estornado") ? "estornar" : "liberar";
        Banco.transacao(c -> {
            Ocorrencia oc = ocorrenciasDao.buscar(c, id, true);
            if (oc == null) throw ErroNegocio.naoEncontrado("Ocorrência não encontrada.");
            if (oc.isResolvida()) throw ErroNegocio.conflito("A ocorrência já foi resolvida.");
            String resolucao = AuditoriaService.corta(m + ": " + o, 400);
            ocorrenciasDao.resolver(c, id, resolucao);

            if (oc.getLoteId() != null) {
                long loteId = oc.getLoteId();
                // Só decide o frete quando não há outra ocorrência do mesmo lote ainda pendente.
                boolean outras = ocorrenciasDao.existemOutrasPendentes(c, loteId, id);
                Movimento mv = movimentos.buscarFreteRetido(c, loteId);
                if (mv != null && !outras) {
                    long mot = mv.getMotoristaId();
                    double valor = mv.getValor();
                    switch (decisao) {
                        case "ajustar" -> {
                            Lote l = lotes.buscarPorId(c, loteId, false);
                            double origem = l.pesoDeOrigem();
                            double fator = l.getConfKg() == null || origem <= 0 ? 1 : Math.min(1, l.getConfKg() / origem);
                            double novo = Math.round(valor * fator * 100) / 100.0;
                            movimentos.ajustarValor(c, mv.getId(), novo);
                            notificacoes.notificar(c, mot, "Ocorrência do lote #" + loteId + " resolvida: frete ajustado pelo peso conferido para R$ "
                                    + String.format(java.util.Locale.ROOT, "%.2f", novo).replace('.', ',') + ".", "fa-scale-balanced");
                        }
                        case "estornar" -> {
                            movimentos.atualizarStatus(c, mv.getId(), "estornado");
                            notificacoes.notificar(c, mot, "Ocorrência do lote #" + loteId + " resolvida: frete estornado. Motivo: " + o, "fa-ban");
                        }
                        default -> {
                            movimentos.atualizarStatus(c, mv.getId(), "disponivel");
                            notificacoes.notificar(c, mot, "Ocorrência do lote #" + loteId + " resolvida sem irregularidade. Frete liberado na carteira.", "fa-sack-dollar");
                        }
                    }
                    lotes.liberarFrete(c, loteId);
                }
                Lote lote = lotes.buscarPorId(c, loteId, false);
                if (lote != null && lote.getOngId() != null) notificacoes.notificar(c, lote.getOngId(), "A ocorrência do lote #" + loteId + " foi resolvida pela administração.", "fa-check");
            }
            auditoria.registrar(c, admin, "Resolveu a ocorrência", "#" + id, null, resolucao);
            return null;
        });
    }
}
