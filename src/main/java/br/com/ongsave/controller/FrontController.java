package br.com.ongsave.controller;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import br.com.ongsave.db.Banco;
import br.com.ongsave.model.ErroNegocio;
import br.com.ongsave.model.Perfil;
import br.com.ongsave.model.Usuario;
import br.com.ongsave.service.AutenticacaoService;
import br.com.ongsave.util.Csrf;
import br.com.ongsave.util.Json;
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
 *   /recuperar-senha (GET/POST) -> public/recuperar-senha.jsp | troca a senha e mostra o aviso antes de ir ao login
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
        urlPatterns = { "", "/login", "/logout", "/cadastro", "/recuperar-senha",
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
            case "/recuperar-senha" -> render(req, resp, "public/recuperar-senha");
            case "/logout" -> logout(req, resp);
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

    private void recuperarSenha(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        String view = "public/recuperar-senha";
        if (!Csrf.valido(req)) { erro(req, resp, view, "A sessão expirou. Atualize a página e tente novamente."); return; }
        try {
            servicos().autenticacao.redefinirSenha(limpar(req.getParameter("email")), req.getParameter("verificacao"),
                    req.getParameter("senha"), req.getParameter("confirmacao"));
        } catch (ErroNegocio e) {
            erro(req, resp, view, e.getMessage());
            return;
        } catch (Banco.ErroBanco e) {
            LOG.log(Level.SEVERE, "Falha ao redefinir a senha", e);
            erro(req, resp, view, "Não foi possível atualizar a senha agora (falha no banco de dados). Tente novamente em instantes.");
            return;
        }
        // Se alguém estava logado neste navegador, a sessão antiga deixa de valer.
        HttpSession s = req.getSession(false);
        if (s != null && s.getAttribute(USUARIO) != null) s.invalidate();
        redirect(req, resp, "/recuperar-senha?ok=1");   // PRG: a página mostra o aviso e leva ao login
    }

    private void logout(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        HttpSession s = req.getSession(false);
        if (s != null) s.invalidate();
        redirect(req, resp, "/login?saiu=1");
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
