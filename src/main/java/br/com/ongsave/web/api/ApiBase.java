package br.com.ongsave.web.api;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import br.com.ongsave.db.Banco;
import br.com.ongsave.model.ErroNegocio;
import br.com.ongsave.model.Perfil;
import br.com.ongsave.model.Usuario;
import br.com.ongsave.util.Csrf;
import br.com.ongsave.util.Json;
import br.com.ongsave.web.Servicos;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.Part;

/**
 * Base de todas as APIs JSON ({@code /api/...}).
 * <ul>
 *   <li>exige sessão iniciada (401) e o perfil certo (403);</li>
 *   <li>valida o token CSRF em todo POST (cabeçalho {@code X-CSRF-Token} ou campo {@code csrf});</li>
 *   <li>revalida a situação da conta a cada minuto: contas suspensas perdem a sessão na hora;</li>
 *   <li>converte {@link ErroNegocio} em status HTTP + {@code {"erro": "..."}} e esconde erros internos.</li>
 * </ul>
 */
public abstract class ApiBase extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private static final Logger LOG = Logger.getLogger(ApiBase.class.getName());

    public static final String USUARIO = "usuarioLogado";
    private static final String VERIFICADO = "ongsave.statusVerificadoEm";
    private static final long REVALIDAR_MS = 60_000;
    private static final int JSON_MAX = 1024 * 1024;

    /** Perfil exigido; {@code null} = qualquer utilizador autenticado. */
    protected abstract Perfil perfil();

    protected Object get(Pedido p) throws IOException, ServletException { throw ErroNegocio.naoEncontrado("Recurso não encontrado."); }

    protected Object post(Pedido p) throws IOException, ServletException { throw ErroNegocio.naoEncontrado("Recurso não encontrado."); }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        tratar(req, resp, false);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        tratar(req, resp, true);
    }

    private void tratar(HttpServletRequest req, HttpServletResponse resp, boolean escrita) throws IOException {
        resp.setHeader("Cache-Control", "no-store");
        try {
            Servicos s = Servicos.de(getServletContext());
            if (s == null) throw new ErroNegocio(503, "Serviço indisponível: banco de dados não configurado.");
            HttpSession sessao = req.getSession(false);
            Usuario u = sessao == null ? null : (Usuario) sessao.getAttribute(USUARIO);
            if (u == null) throw new ErroNegocio(401, "Sessão expirada. Entre novamente.");
            if (perfil() != null && u.getPerfil() != perfil()) throw ErroNegocio.proibido("Acesso não permitido para este perfil.");
            if (escrita && !Csrf.valido(req)) throw ErroNegocio.proibido("Sessão expirada ou pedido inválido. Atualize a página.");
            revalidar(s, sessao, u);

            Pedido p = new Pedido(req, resp, u, s);
            Object r = escrita ? post(p) : get(p);
            if (r instanceof Arquivo a) { enviarArquivo(resp, a); return; }
            responder(resp, 200, r == null ? Json.obj("ok", true) : r);
        } catch (ErroNegocio e) {
            responder(resp, e.getStatus(), Json.obj("erro", e.getMessage()));
        } catch (Banco.ErroBanco e) {
            LOG.log(Level.SEVERE, "Erro de banco em " + req.getRequestURI(), e);
            responder(resp, 503, Json.obj("erro", "Não foi possível falar com o banco de dados agora. Tente de novo em instantes."));
        } catch (IllegalArgumentException e) {
            responder(resp, 400, Json.obj("erro", "Pedido inválido."));
        } catch (ServletException | IllegalStateException e) {
            // Inclui uploads acima do limite do @MultipartConfig.
            LOG.log(Level.WARNING, "Pedido rejeitado em " + req.getRequestURI() + ": " + e.getMessage());
            responder(resp, 413, Json.obj("erro", "O arquivo enviado é grande demais."));
        } catch (RuntimeException e) {
            LOG.log(Level.SEVERE, "Erro inesperado em " + req.getRequestURI(), e);
            responder(resp, 500, Json.obj("erro", "Erro inesperado. A equipa técnica foi avisada pelos registos do servidor."));
        }
    }

    /** Contas suspensas/bloqueadas perdem a sessão no máximo um minuto depois da decisão do admin. */
    private static void revalidar(Servicos s, HttpSession sessao, Usuario u) {
        Long em = (Long) sessao.getAttribute(VERIFICADO);
        long agora = System.currentTimeMillis();
        if (em != null && agora - em < REVALIDAR_MS) return;
        String st = s.autenticacao.status(u.getId());
        if (!"ativo".equals(st)) {
            sessao.invalidate();
            throw new ErroNegocio(401, "A sua conta não está ativa. Entre novamente ou fale com o suporte.");
        }
        sessao.setAttribute(VERIFICADO, agora);
    }

    private static void responder(HttpServletResponse resp, int status, Object corpo) throws IOException {
        if (resp.isCommitted()) return;
        resp.setStatus(status);
        resp.setContentType("application/json;charset=UTF-8");
        byte[] b = Json.escrever(corpo).getBytes(StandardCharsets.UTF_8);
        resp.setContentLength(b.length);
        try (OutputStream o = resp.getOutputStream()) { o.write(b); }
    }

    private static void enviarArquivo(HttpServletResponse resp, Arquivo a) throws IOException {
        resp.setContentType(a.tipo() == null ? "application/octet-stream" : a.tipo());
        resp.setHeader("Cache-Control", "private, max-age=300");
        resp.setHeader("Content-Security-Policy", "default-src 'none'; img-src 'self'; style-src 'unsafe-inline'; sandbox");
        if (a.nome() != null)
            resp.setHeader("Content-Disposition", "inline; filename=\"" + a.nome().replaceAll("[^A-Za-z0-9._-]", "_") + "\"");
        resp.setContentLength(a.bytes().length);
        try (OutputStream o = resp.getOutputStream()) { o.write(a.bytes()); }
    }

    /** Resposta binária (fotos e documentos). */
    public record Arquivo(byte[] bytes, String tipo, String nome) {}

    /** Pedido já autenticado, com leitura unificada de JSON, formulário e multipart. */
    public static final class Pedido {
        public final HttpServletRequest req;
        public final HttpServletResponse resp;
        public final Usuario usuario;
        public final Servicos s;
        /** Segmentos do caminho depois do servlet: /lotes/12/cancelar -> [lotes, 12, cancelar]. */
        public final List<String> rota;
        private Map<String, Object> json;

        Pedido(HttpServletRequest req, HttpServletResponse resp, Usuario usuario, Servicos s) throws IOException {
            this.req = req;
            this.resp = resp;
            this.usuario = usuario;
            this.s = s;
            String info = req.getPathInfo();
            List<String> r = new ArrayList<>();
            if (info != null) for (String seg : info.split("/")) if (!seg.isEmpty()) r.add(seg);
            this.rota = List.copyOf(r);
            String ct = req.getContentType();
            if ("POST".equals(req.getMethod()) && ct != null && ct.toLowerCase().startsWith("application/json")) {
                String corpo = new String(lerLimitado(req.getInputStream(), JSON_MAX), StandardCharsets.UTF_8);
                json = corpo.isBlank() ? Map.of() : Json.lerObjeto(corpo);
            }
        }

        /** Verifica a rota: {@code p.e("lotes", "*", "cancelar")} ("*" casa qualquer segmento). */
        public boolean e(String... partes) {
            if (partes.length != rota.size()) return false;
            for (int i = 0; i < partes.length; i++) if (!"*".equals(partes[i]) && !partes[i].equals(rota.get(i))) return false;
            return true;
        }

        public long id(int segmento) {
            try { return Long.parseLong(rota.get(segmento)); }
            catch (RuntimeException e) { throw ErroNegocio.naoEncontrado("Recurso não encontrado."); }
        }

        /** Valor textual de um campo (JSON ou formulário). */
        public String p(String nome) {
            if (json != null) {
                Object v = json.get(nome);
                if (v == null) return null;
                if (v instanceof Double d && d == Math.rint(d) && !Double.isInfinite(d)) return String.valueOf(d.longValue());
                return v.toString();
            }
            return req.getParameter(nome);
        }

        public double num(String nome) {
            String v = p(nome);
            try { return v == null ? Double.NaN : Double.parseDouble(v.replace(',', '.')); }
            catch (NumberFormatException e) { return Double.NaN; }
        }

        public boolean bool(String nome) {
            String v = p(nome);
            return v != null && (v.equals("true") || v.equals("on") || v.equals("1") || v.equals("sim"));
        }

        /** Lista de textos: array no JSON ou campo repetido no formulário. */
        public List<String> lista(String nome) {
            if (json != null) {
                Object v = json.get(nome);
                List<String> r = new ArrayList<>();
                if (v instanceof List<?> l) for (Object o : l) if (o != null) r.add(o.toString());
                return r;
            }
            String[] v = req.getParameterValues(nome);
            return v == null ? List.of() : Arrays.asList(v);
        }

        /** Objeto JSON inteiro (para parâmetros da plataforma). */
        public Map<String, Object> corpo() {
            return json == null ? new LinkedHashMap<>() : json;
        }

        /** Campos simples num mapa (para os formulários de perfil). */
        public Map<String, String> campos(String... nomes) {
            Map<String, String> m = new LinkedHashMap<>();
            for (String n : nomes) m.put(n, p(n));
            return m;
        }

        /** Conteúdo de um ficheiro multipart, ou {@code null}. */
        public byte[] arquivo(String nome, int maxBytes) throws IOException, ServletException {
            Part part = req.getPart(nome);
            if (part == null || part.getSize() == 0) return null;
            if (part.getSize() > maxBytes) throw ErroNegocio.invalido("O arquivo passa de " + (maxBytes / (1024 * 1024)) + " MB.");
            try (InputStream in = part.getInputStream()) { return lerLimitado(in, maxBytes); }
        }

        public String nomeArquivo(String nome) throws IOException, ServletException {
            Part part = req.getPart(nome);
            return part == null ? null : part.getSubmittedFileName();
        }

        /** Mantém o nome mostrado no topo das páginas em sincronia depois de editar o perfil. */
        public void atualizarNomeSessao(String nome) {
            HttpSession s = req.getSession(false);
            if (s != null && nome != null && !nome.isBlank()) s.setAttribute(USUARIO, usuario.comNome(nome));
        }
    }

    static byte[] lerLimitado(InputStream in, int max) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        int n, total = 0;
        while ((n = in.read(buf)) != -1) {
            total += n;
            if (total > max) throw ErroNegocio.invalido("O conteúdo enviado é grande demais.");
            out.write(buf, 0, n);
        }
        return out.toByteArray();
    }
}
