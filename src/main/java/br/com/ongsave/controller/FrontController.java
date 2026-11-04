package br.com.ongsave.controller;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import br.com.ongsave.db.Banco;
import br.com.ongsave.model.ErroNegocio;
import br.com.ongsave.model.Perfil;
import br.com.ongsave.model.Usuario;
import br.com.ongsave.service.AutenticacaoService;
import br.com.ongsave.util.Csrf;
import br.com.ongsave.util.Json;
import br.com.ongsave.util.LimiteTaxa;
import br.com.ongsave.web.DemoFilter;
import br.com.ongsave.web.Servicos;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

/**
 * Controlador frontal do OngSave.
 *
 *   /                     -> public/index.jsp
 *   /login  (GET/POST)    -> public/login.jsp  | autentica e redireciona para o painel do perfil
 *   /cadastro (GET/POST)  -> public/cadastro.jsp | cria conta e redireciona para /login?cadastro=ok
 *   /logout               -> encerra a sessão
 *   /recuperar-senha (GET/POST) -> public/recuperar-senha.jsp | pede o link por e-mail; com ?token= grava a nova senha
 *   /demo                 -> public/demo.jsp (cartões de cada perfil; só com app.demo=true)
 *   /demo/{perfil}        -> entra sem senha na conta de demonstração do perfil e abre o painel
 *   /{perfil}/{pagina}    -> /WEB-INF/jsp/{perfil}/{pagina}.jsp (só para quem tem esse perfil)
 *
 * Cada página de painel recebe o atributo {@code dadosJson}: o estado inicial do perfil vindo do banco
 * (o mesmo JSON de GET /api/{perfil}/estado), os parâmetros da plataforma e a configuração de geolocalização.
 * O fragmento WEB-INF/jspf/dados.jspf coloca-o num {@code <script type="application/json">} lido pelo plataforma.js.
 * As ações dos painéis vão para as APIs JSON em /api/* (pacote br.com.ongsave.web.api).
 *
 * As JSPs ficam em WEB-INF: ninguém as abre diretamente, só através deste servlet.
 * CSS/JS/imagens em /assets/* são servidos pelo servlet padrão do Tomcat.
 */
@WebServlet(name = "FrontController",
        urlPatterns = { "", "/login", "/logout", "/cadastro", "/recuperar-senha", "/demo/*",
                        "/empresa/*", "/motorista/*", "/ong/*", "/admin/*" })
public class FrontController extends HttpServlet {
    private static final long serialVersionUID = 1L;

    private static final String JSP = "/WEB-INF/jsp/";
    private static final String USUARIO = "usuarioLogado";
    private static final Logger LOG = Logger.getLogger(FrontController.class.getName());

    /** Páginas permitidas por perfil (lista branca = nada de caminhos arbitrários). */
    private static final Map<Perfil, Set<String>> PAGINAS = Map.of(
            Perfil.EMPRESA,   Set.of("dashboard", "nova-doacao", "perfil", "relatorio-esg"),
            Perfil.MOTORISTA, Set.of("dashboard", "entregas", "rota", "historico", "carteira", "perfil"),
            Perfil.ONG,       Set.of("dashboard", "confirmar-entrega", "perfil"),
            Perfil.ADMIN,     Set.of("dashboard", "gerir-utilizadores", "detalhe-utilizador", "financeiro"));

    /* Limites por IP contra força bruta e abuso (o bloqueio por conta continua a existir). */
    private static final LimiteTaxa LIMITE_LOGIN = new LimiteTaxa(10, 5 * 60_000L);
    private static final LimiteTaxa LIMITE_CADASTRO = new LimiteTaxa(5, 60 * 60_000L);
    private static final LimiteTaxa LIMITE_RECUPERAR = new LimiteTaxa(5, 15 * 60_000L);
    private static final LimiteTaxa LIMITE_DEMO = new LimiteTaxa(40, 60_000L);

    /** Contas de demonstração (criadas pelo V002__dados_iniciais.sql), uma por perfil. */
    private static final Map<Perfil, String> CONTAS_DEMO = Map.of(
            Perfil.EMPRESA, "empresa@ongsave.com",
            Perfil.MOTORISTA, "motorista@ongsave.com",
            Perfil.ONG, "ong@ongsave.com",
            Perfil.ADMIN, "admin@ongsave.com");

    /** Serviços criados pelo Inicializador. Se o banco não arrancou, o SegurancaFilter já respondeu com 503. */
    private Servicos servicos() {
        Servicos s = Servicos.de(getServletContext());
        if (s == null) throw new IllegalStateException("Aplicação sem banco de dados configurado.");
        return s;
    }

    /* ======================= GET ======================= */

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        switch (req.getServletPath()) {
            case "", "/" -> render(req, resp, "public/index");
            case "/login" -> {
                Usuario u = usuarioLogado(req);
                if (u != null) redirect(req, resp, "/" + u.getPerfil().getSegmento() + "/dashboard");
                else render(req, resp, "public/login");
            }
            case "/cadastro" -> render(req, resp, "public/cadastro");
            case "/recuperar-senha" -> {
                String token = req.getParameter("token");
                if (token != null) req.setAttribute("tokenValido", servicos().autenticacao.tokenValido(token));
                resp.setHeader("Referrer-Policy", "no-referrer");          // o token não vaza para sites externos
                render(req, resp, "public/recuperar-senha");
            }
            case "/logout" -> logout(req, resp);
            case "/demo" -> demo(req, resp);
            default -> painel(req, resp);
        }
    }

    /* ======================= POST ====================== */

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        switch (req.getServletPath()) {
            case "/login" -> login(req, resp);
            case "/cadastro" -> cadastro(req, resp);
            case "/recuperar-senha" -> recuperarSenha(req, resp);
            // As ações dos painéis vão para as APIs JSON em /api/*.
            default -> resp.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
        }
    }

    /* ===================== Ações ======================= */

    private void painel(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        String segmento = req.getServletPath().substring(1);          // "empresa"
        Perfil perfil = Perfil.doSegmento(segmento).orElse(null);
        if (perfil == null) { resp.sendError(HttpServletResponse.SC_NOT_FOUND); return; }

        String info = req.getPathInfo();                              // "/dashboard"
        if (info == null || "/".equals(info)) {                       // /empresa -> /empresa/dashboard
            redirect(req, resp, "/" + segmento + "/dashboard");
            return;
        }
        String pagina = info.substring(1);
        if (!PAGINAS.get(perfil).contains(pagina)) { resp.sendError(HttpServletResponse.SC_NOT_FOUND); return; }

        Usuario u = usuarioLogado(req);
        if (u == null) {
            String destino = req.getRequestURI() + (req.getQueryString() != null ? "?" + req.getQueryString() : "");
            redirect(req, resp, "/login?next=" + URLEncoder.encode(destino, StandardCharsets.UTF_8));
            return;
        }
        if (u.getPerfil() != perfil) { resp.sendError(HttpServletResponse.SC_FORBIDDEN); return; }

        Servicos s = servicos();
        // Conta suspensa/bloqueada depois do login: a sessão cai na próxima página aberta.
        if (!"ativo".equals(s.autenticacao.status(u.getId()))) {
            req.getSession().invalidate();
            redirect(req, resp, "/login?inativa=1");
            return;
        }
        Map<String, Object> dados = new LinkedHashMap<>();
        dados.put("usuario", Json.obj("id", u.getId(), "nome", u.getNome(), "email", u.getEmail(), "perfil", segmento));
        dados.put("plat", s.plataforma.config());
        dados.put("geo", Json.obj("validar", s.config.geoValidar(), "raioM", s.config.raioValidacaoMetros()));
        dados.put("sim", Json.obj("ativo", s.config.simulacao(), "intervaloMs", s.config.simIntervaloMs()));
        dados.put("demo", s.config.demo() && req.getSession().getAttribute(DemoFilter.CONTAS) != null);
        try {
            dados.put("estado", switch (perfil) {
                case EMPRESA -> s.empresa.estado(u);
                case MOTORISTA -> s.motorista.estado(u);
                case ONG -> s.ong.estado(u);
                case ADMIN -> s.admin.estado(u);
            });
        } catch (Banco.ErroBanco e) {
            LOG.log(Level.SEVERE, "Falha ao carregar o painel " + segmento, e);
            resp.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
            return;
        }
        req.setAttribute("dadosJson", Json.escrever(dados));
        req.setAttribute("paginaAtual", pagina);
        render(req, resp, segmento + "/" + pagina);
    }

    private void login(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        if (!Csrf.valido(req)) {
            erro(req, resp, "public/login", "A sessão expirou. Tente novamente.");
            return;
        }
        if (!LIMITE_LOGIN.permitir(req.getRemoteAddr())) {
            muitasTentativas(req, resp, "public/login", LIMITE_LOGIN);
            return;
        }
        AutenticacaoService.Resultado r = servicos().autenticacao.autenticar(req.getParameter("email"), req.getParameter("senha"));
        if (r.usuario() == null) {
            erro(req, resp, "public/login", r.erro());
            return;
        }
        Usuario u = r.usuario();
        // Nova sessão após o login (evita fixação de sessão)
        HttpSession antiga = req.getSession(false);
        if (antiga != null) antiga.invalidate();
        HttpSession sessao = req.getSession(true);
        sessao.setAttribute(USUARIO, u);
        Csrf.garantir(sessao);

        String next = req.getParameter("next");
        if (destinoSeguro(req, next)) resp.sendRedirect(next);
        else redirect(req, resp, "/" + u.getPerfil().getSegmento() + "/dashboard");
    }

    private void cadastro(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        if (!Csrf.valido(req)) { erro(req, resp, "public/cadastro", "A sessão expirou. Tente novamente."); return; }

        if (!LIMITE_CADASTRO.permitir(req.getRemoteAddr())) { muitasTentativas(req, resp, "public/cadastro", LIMITE_CADASTRO); return; }
        if (req.getParameter("termos") == null) { erro(req, resp, "public/cadastro", "É preciso aceitar os termos."); return; }
        Perfil perfil = Perfil.doSegmento(limpar(req.getParameter("perfil"))).orElse(null);
        try {
            servicos().autenticacao.registrar(new AutenticacaoService.Cadastro(perfil, limpar(req.getParameter("nome")),
                    limpar(req.getParameter("email")), req.getParameter("senha"), limpar(req.getParameter("documento")),
                    limpar(req.getParameter("telefone")), limpar(req.getParameter("extra")), limpar(req.getParameter("cnh")),
                    limpar(req.getParameter("categoriaCnh")).toUpperCase(), limpar(req.getParameter("placa"))));
        } catch (ErroNegocio e) {
            erro(req, resp, "public/cadastro", e.getMessage());
            return;
        }
        redirect(req, resp, "/login?cadastro=ok");   // PRG: evita reenvio ao atualizar
    }

    /**
     * Recuperação de senha em dois passos:
     *   1) POST com "email"  -> envia um link de uso único (30 min) e mostra SEMPRE a mesma mensagem;
     *   2) POST com "token"  -> grava a nova senha e leva ao login.
     */
    private void recuperarSenha(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        String view = "public/recuperar-senha";
        if (!Csrf.valido(req)) { erro(req, resp, view, "A sessão expirou. Atualize a página e tente novamente."); return; }
        if (!LIMITE_RECUPERAR.permitir(req.getRemoteAddr())) { muitasTentativas(req, resp, view, LIMITE_RECUPERAR); return; }
        Servicos s = servicos();
        String token = req.getParameter("token");
        try {
            if (token != null) {
                req.setAttribute("tokenValido", true);
                s.autenticacao.redefinirComToken(token, req.getParameter("senha"), req.getParameter("confirmacao"));
            } else {
                String base = urlBase(req, s);
                if (base == null) LOG.severe("OngSave: pedido de recuperação ignorado: defina app.urlPublica para enviar os links.");
                else s.autenticacao.solicitarRecuperacao(limpar(req.getParameter("email")), base, req.getRemoteAddr(), s.emails);
            }
        } catch (ErroNegocio e) {
            erro(req, resp, view, e.getMessage());
            return;
        } catch (Banco.ErroBanco e) {
            LOG.log(Level.SEVERE, "Falha na recuperação de senha", e);
            erro(req, resp, view, "Não foi possível concluir agora (falha no banco de dados). Tente novamente em instantes.");
            return;
        }
        if (token == null) { redirect(req, resp, "/recuperar-senha?enviado=1"); return; }
        // Senha trocada: qualquer sessão aberta neste navegador deixa de valer.
        HttpSession sessao = req.getSession(false);
        if (sessao != null) sessao.invalidate();
        redirect(req, resp, "/recuperar-senha?ok=1");
    }

    /**
     * Endereço usado nos links de e-mail. Com envio real, só o app.urlPublica configurado (nunca o Host do pedido,
     * que um atacante controla). Sem envio real (links só no log), serve o endereço do próprio pedido.
     */
    private static String urlBase(HttpServletRequest req, Servicos s) {
        String cfg = s.config.urlPublica();
        if (!cfg.isEmpty()) return cfg;
        if (s.emails.configurado()) return null;
        return req.getScheme() + "://" + req.getServerName()
                + ((req.getServerPort() == 80 || req.getServerPort() == 443) ? "" : ":" + req.getServerPort()) + req.getContextPath();
    }

    private void muitasTentativas(HttpServletRequest req, HttpServletResponse resp, String view, LimiteTaxa limite)
            throws ServletException, IOException {
        long seg = limite.segundosParaLiberar(req.getRemoteAddr());
        resp.setHeader("Retry-After", String.valueOf(seg));
        req.setAttribute("erro", "Muitas tentativas seguidas a partir desta rede. Aguarde " + Math.max(1, (seg + 59) / 60)
                + " minuto(s) e tente de novo.");
        resp.setStatus(429);
        render(req, resp, view);
    }

    @SuppressWarnings("unchecked")
    private void logout(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        HttpSession s = req.getSession(false);
        Object contas = s == null ? null : s.getAttribute(DemoFilter.CONTAS);
        if (contas instanceof Map<?, ?> mapa) {
            // Demonstração: sai só do perfil desta aba; os outros perfis abertos continuam.
            Usuario atual = usuarioLogado(req);
            if (atual != null) mapa.remove(atual.getPerfil().getSegmento());
            if (mapa.isEmpty()) s.invalidate();
            else s.setAttribute(DemoFilter.ULTIMO, ((Map<String, Usuario>) mapa).keySet().iterator().next());
            redirect(req, resp, "/demo");
            return;
        }
        if (s != null) s.invalidate();
        redirect(req, resp, "/login?saiu=1");
    }

    /**
     * /demo mostra os cartões; /demo/{perfil} entra (sem senha) na conta de demonstração desse perfil.
     * Os perfis acumulam-se na mesma sessão (ver {@link DemoFilter}), por isso dá para ter a empresa, a ONG,
     * o motorista e o admin abertos em abas diferentes do mesmo navegador.
     */
    @SuppressWarnings("unchecked")
    private void demo(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        Servicos s = servicos();
        if (!s.config.demo()) { resp.sendError(HttpServletResponse.SC_NOT_FOUND); return; }

        String info = req.getPathInfo();
        if (info == null || "/".equals(info)) {
            req.setAttribute("simAtivo", s.config.simulacao());
            render(req, resp, "public/demo");
            return;
        }
        Perfil perfil = Perfil.doSegmento(info.substring(1)).orElse(null);
        if (perfil == null) { resp.sendError(HttpServletResponse.SC_NOT_FOUND); return; }
        if (!LIMITE_DEMO.permitir(req.getRemoteAddr())) {
            req.setAttribute("simAtivo", s.config.simulacao());
            muitasTentativas(req, resp, "public/demo", LIMITE_DEMO);
            return;
        }

        Usuario u;
        try {
            u = s.autenticacao.entrarDemo(CONTAS_DEMO.get(perfil));
        } catch (ErroNegocio e) {
            req.setAttribute("simAtivo", s.config.simulacao());
            erro(req, resp, "public/demo", e.getMessage());
            return;
        } catch (Banco.ErroBanco e) {
            LOG.log(Level.SEVERE, "Falha ao entrar na demonstração", e);
            req.setAttribute("simAtivo", s.config.simulacao());
            erro(req, resp, "public/demo", "Não foi possível falar com o banco de dados agora. Tente de novo em instantes.");
            return;
        }

        // Quem tinha uma sessão normal (login com senha) começa uma sessão nova só de demonstração.
        HttpSession sessao = req.getSession(false);
        if (sessao != null && sessao.getAttribute(DemoFilter.CONTAS) == null && sessao.getAttribute(USUARIO) != null) {
            sessao.invalidate();
            sessao = null;
        }
        if (sessao == null) sessao = req.getSession(true);
        Map<String, Usuario> contas = (Map<String, Usuario>) sessao.getAttribute(DemoFilter.CONTAS);
        if (contas == null) contas = new ConcurrentHashMap<>();
        contas.put(perfil.getSegmento(), u);
        sessao.setAttribute(DemoFilter.CONTAS, contas);
        sessao.setAttribute(DemoFilter.ULTIMO, perfil.getSegmento());
        sessao.removeAttribute(USUARIO);                  // na demo o utilizador vem do mapa (DemoFilter)
        sessao.setMaxInactiveInterval(2 * 60 * 60);       // apresentações longas: 2 h sem cair
        Csrf.garantir(sessao);
        redirect(req, resp, "/" + perfil.getSegmento() + "/dashboard");
    }

    /* ==================== Utilitários ==================== */

    private void render(HttpServletRequest req, HttpServletResponse resp, String view)
            throws ServletException, IOException {
        Csrf.garantir(req.getSession(true));
        resp.setHeader("Cache-Control", "no-store");   // painéis não ficam em cache após logout
        req.getRequestDispatcher(JSP + view + ".jsp").forward(req, resp);
    }

    private void erro(HttpServletRequest req, HttpServletResponse resp, String view, String msg)
            throws ServletException, IOException {
        req.setAttribute("erro", msg);
        resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        render(req, resp, view);
    }

    private static void redirect(HttpServletRequest req, HttpServletResponse resp, String caminho) throws IOException {
        resp.sendRedirect(req.getContextPath() + caminho);
    }

    private static Usuario usuarioLogado(HttpServletRequest req) {
        HttpSession s = req.getSession(false);
        return s == null ? null : (Usuario) s.getAttribute(USUARIO);
    }

    /** Só aceita "next" que aponte para dentro da própria aplicação (evita open redirect). */
    private static boolean destinoSeguro(HttpServletRequest req, String next) {
        return next != null && next.startsWith(req.getContextPath() + "/")
                && !next.startsWith("//") && !next.contains("\\") && !next.contains("\r") && !next.contains("\n");
    }

    private static String limpar(String s) {
        return s == null ? "" : s.trim();
    }
}
