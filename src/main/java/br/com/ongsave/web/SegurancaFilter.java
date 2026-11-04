package br.com.ongsave.web;

import java.io.IOException;
import java.io.PrintWriter;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Cabeçalhos de segurança em todas as respostas e, se o arranque falhou (banco não configurado,
 * driver ausente, credenciais erradas), uma página 503 que explica exatamente o que corrigir.
 */
@WebFilter(filterName = "SegurancaFilter", urlPatterns = "/*")
public class SegurancaFilter implements Filter {

    /**
     * Content-Security-Policy: só deixa carregar scripts, estilos, fontes e imagens da própria aplicação e das CDNs
     * usadas pelas páginas, e só deixa o JavaScript falar com o servidor, o OSRM (rotas) e o ViaCEP.
     * Bloqueia plugins, {@code <base>} injetado, formulários para outros sites e a página dentro de iframes alheios.
     * 'unsafe-inline' continua necessário por causa dos scripts/estilos embutidos nas JSPs e do Tailwind Play CDN.
     */
    static final String CSP = String.join("; ",
            "default-src 'self'",
            "script-src 'self' 'unsafe-inline' https://cdn.tailwindcss.com https://cdnjs.cloudflare.com https://unpkg.com",
            "style-src 'self' 'unsafe-inline' https://cdnjs.cloudflare.com https://unpkg.com https://fonts.googleapis.com",
            "font-src 'self' data: https://cdnjs.cloudflare.com https://fonts.gstatic.com",
            "img-src 'self' data: blob: https://tile.openstreetmap.org https://*.tile.openstreetmap.org https://unpkg.com https://images.unsplash.com",
            "connect-src 'self' https://router.project-osrm.org https://viacep.com.br",
            "media-src 'self' blob:",
            "worker-src 'self' blob:",
            "object-src 'none'",
            "base-uri 'self'",
            "form-action 'self'",
            "frame-ancestors 'self'");

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse resp = (HttpServletResponse) response;

        resp.setHeader("X-Content-Type-Options", "nosniff");
        resp.setHeader("X-Frame-Options", "SAMEORIGIN");
        resp.setHeader("Referrer-Policy", "strict-origin-when-cross-origin");
        resp.setHeader("Permissions-Policy", "geolocation=(self), camera=(self), microphone=(), payment=(), usb=()");
        resp.setHeader("Cross-Origin-Opener-Policy", "same-origin");
        resp.setHeader("X-Permitted-Cross-Domain-Policies", "none");
        resp.setHeader("Content-Security-Policy", req.isSecure() ? CSP + "; upgrade-insecure-requests" : CSP);
        // Atrás de um proxy HTTPS (Render, Railway...), o RemoteIpValve do META-INF/context.xml faz isSecure() = true.
        if (req.isSecure()) resp.setHeader("Strict-Transport-Security", "max-age=31536000; includeSubDomains");

        String caminho = req.getRequestURI().substring(req.getContextPath().length());
        String erro = (String) req.getServletContext().getAttribute(Inicializador.ERRO);
        if (erro != null && !caminho.startsWith("/assets/")) {
            resp.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
            resp.setHeader("Cache-Control", "no-store");
            if (caminho.startsWith("/api/")) {
                resp.setContentType("application/json;charset=UTF-8");
                resp.getWriter().write("{\"erro\":\"Serviço indisponível: o banco de dados não está configurado.\"}");
                return;
            }
            resp.setContentType("text/html;charset=UTF-8");
            PrintWriter w = resp.getWriter();
            w.write("<!DOCTYPE html><html lang=\"pt-BR\"><head><meta charset=\"UTF-8\"><meta name=\"viewport\" content=\"width=device-width,initial-scale=1\">"
                    + "<title>OngSave · configuração necessária</title><style>body{font-family:system-ui,sans-serif;background:#f8fafc;color:#0f172a;"
                    + "display:grid;place-items:center;min-height:100vh;margin:0;padding:24px}main{max-width:640px;background:#fff;border:1px solid #e2e8f0;"
                    + "border-radius:24px;padding:32px}h1{font-size:22px;margin:0 0 12px}p{line-height:1.6;color:#334155}code{background:#f1f5f9;"
                    + "padding:2px 6px;border-radius:6px}.erro{background:#fef2f2;border:1px solid #fecaca;color:#991b1b;border-radius:12px;padding:12px 16px}"
                    + "</style></head><body><main><h1>OngSave ainda não está ligado ao banco de dados</h1><p class=\"erro\">"
                    + escapar(erro) + "</p><p>Passo a passo:</p><ol><li>No painel do Neon, clique em <b>Connect</b> e copie a <i>connection string</i>.</li>"
                    + "<li>Cole-a em <code>WEB-INF/ongsave.properties</code> na linha <code>db.url=</code>.</li>"
                    + "<li>Reinicie o Tomcat. As tabelas são criadas automaticamente.</li></ol>"
                    + "<p>Os detalhes técnicos estão no log do Tomcat (consola do Eclipse).</p></main></body></html>");
            return;
        }
        chain.doFilter(request, response);
    }

    private static String escapar(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }
}
