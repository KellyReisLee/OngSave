package br.com.ongsave.web;

import java.io.IOException;
import java.net.URI;
import java.util.Enumeration;
import java.util.Map;

import br.com.ongsave.model.Usuario;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpSession;

/**
 * Página de demonstração com os quatro perfis abertos AO MESMO TEMPO no mesmo navegador.
 *
 * <p>Normalmente a sessão guarda um único "usuarioLogado". Quem entra pela página /demo ganha na sessão
 * um mapa {@code perfil -> conta de demonstração}. Este filtro escolhe, a cada pedido, a conta do perfil
 * do endereço pedido ({@code /empresa/...}, {@code /api/ong/...}). Para {@code /api/conta/...} (comum a
 * todos) usa a página de onde veio o pedido (Referer) e, sem ela, o último perfil aberto.</p>
 *
 * <p>Nada é alterado na sessão real: o pedido é embrulhado e só a leitura de "usuarioLogado" muda.
 * Assim, várias abas a atualizar em paralelo nunca trocam de utilizador umas às outras.</p>
 */
@WebFilter(filterName = "DemoFilter", urlPatterns = "/*")
public class DemoFilter implements Filter {

    /** Mapa (segmento do perfil -> Usuario) das contas de demonstração abertas nesta sessão. */
    public static final String CONTAS = "ongsave.demoContas";
    /** Último perfil aberto pela página de demonstração. */
    public static final String ULTIMO = "ongsave.demoUltimo";
    private static final String USUARIO = "usuarioLogado";

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpSession sessao = req.getSession(false);
        Object contas = sessao == null ? null : sessao.getAttribute(CONTAS);
        if (!(contas instanceof Map<?, ?> mapa) || mapa.isEmpty()) {
            chain.doFilter(request, response);
            return;
        }
        Object u = mapa.get(perfilDoPedido(req, sessao));
        if (u instanceof Usuario usuario) chain.doFilter(new Pedido(req, usuario), response);
        else chain.doFilter(request, response);
    }

    /** "empresa" para /empresa/... e /api/empresa/...; para os restantes, o perfil da página de origem. */
    private static String perfilDoPedido(HttpServletRequest req, HttpSession sessao) {
        String ctx = req.getContextPath();
        String seg = primeiroSegmento(req.getRequestURI().substring(ctx.length()));
        if (ehPerfil(seg)) return seg;
        String ref = req.getHeader("Referer");
        if (ref != null) {
            try {
                String caminho = URI.create(ref).getPath();
                if (caminho != null && caminho.startsWith(ctx)) {
                    String r = primeiroSegmento(caminho.substring(ctx.length()));
                    if (ehPerfil(r)) return r;
                }
            } catch (IllegalArgumentException ignorar) { /* Referer inválido */ }
        }
        Object ultimo = sessao.getAttribute(ULTIMO);
        return ultimo == null ? "" : ultimo.toString();
    }

    private static String primeiroSegmento(String caminho) {
        String[] partes = caminho.split("/");
        int i = 0;
        while (i < partes.length && partes[i].isEmpty()) i++;
        if (i < partes.length && "api".equals(partes[i])) i++;
        return i < partes.length ? partes[i] : "";
    }

    private static boolean ehPerfil(String s) {
        return "empresa".equals(s) || "motorista".equals(s) || "ong".equals(s) || "admin".equals(s);
    }

    /* ---------- Embrulhos: a mesma sessão, mas "usuarioLogado" = conta do perfil deste pedido ---------- */

    private static final class Pedido extends HttpServletRequestWrapper {
        private final Usuario usuario;

        Pedido(HttpServletRequest req, Usuario usuario) {
            super(req);
            this.usuario = usuario;
        }

        @Override
        public HttpSession getSession(boolean criar) {
            HttpSession s = super.getSession(criar);
            return s == null ? null : new Sessao(s, usuario);
        }

        @Override
        public HttpSession getSession() {
            return getSession(true);
        }
    }

    private static final class Sessao implements HttpSession {
        private final HttpSession real;
        private final Usuario usuario;

        Sessao(HttpSession real, Usuario usuario) {
            this.real = real;
            this.usuario = usuario;
        }

        @Override
        public Object getAttribute(String nome) {
            if (USUARIO.equals(nome)) {
                // Se a sessão foi encerrada (logout) ou trocada por um login normal, deixa de valer.
                Object contas = real.getAttribute(CONTAS);
                return contas instanceof Map<?, ?> m && m.containsValue(usuario) ? usuario : real.getAttribute(nome);
            }
            return real.getAttribute(nome);
        }

        @Override public long getCreationTime() { return real.getCreationTime(); }
        @Override public String getId() { return real.getId(); }
        @Override public long getLastAccessedTime() { return real.getLastAccessedTime(); }
        @Override public ServletContext getServletContext() { return real.getServletContext(); }
        @Override public void setMaxInactiveInterval(int intervalo) { real.setMaxInactiveInterval(intervalo); }
        @Override public int getMaxInactiveInterval() { return real.getMaxInactiveInterval(); }
        @Override public Enumeration<String> getAttributeNames() { return real.getAttributeNames(); }
        @Override
        @SuppressWarnings("unchecked")
        public void setAttribute(String nome, Object valor) {
            // Ex.: o nome foi atualizado no perfil. Na demo a conta vive no mapa, não no atributo normal da sessão
            // (senão o "Encerrar sessão" de uma aba deixaria essa conta presa na sessão).
            if (USUARIO.equals(nome) && valor instanceof Usuario novo && real.getAttribute(CONTAS) instanceof Map<?, ?> m) {
                ((Map<String, Usuario>) m).put(novo.getPerfil().getSegmento(), novo);
                return;
            }
            real.setAttribute(nome, valor);
        }
        @Override public void removeAttribute(String nome) { real.removeAttribute(nome); }
        @Override public void invalidate() { real.invalidate(); }
        @Override public boolean isNew() { return real.isNew(); }
    }
}
