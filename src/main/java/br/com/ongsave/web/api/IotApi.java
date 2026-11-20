package br.com.ongsave.web.api;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import br.com.ongsave.db.Banco;
import br.com.ongsave.model.ErroNegocio;
import br.com.ongsave.util.Json;
import br.com.ongsave.util.LimiteTaxa;
import br.com.ongsave.web.Servicos;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * API dos sensores IoT. Os dispositivos não têm sessão: autenticam-se com a chave {@code iot.chave}
 * no cabeçalho {@code X-Sensor-Key} (comparação em tempo constante).
 * <pre>
 * GET  /api/iot/sensores                       sensores ativos, última leitura e lote em pesagem (balanças)
 * GET  /api/iot/sensores/{id}/leituras?limite  últimas leituras de um sensor (máx. 200)
 * POST /api/iot/leituras                       JSON: {"sensor": "...", "valor": 4.2, "lote": 123}  (lote só na balança)
 * </pre>
 * Respostas: 200, 401 (chave em falta ou errada), 404, 405, 413, 422, 429, 503 (IoT desligado ou banco indisponível).
 */
@WebServlet(name = "IotApi", urlPatterns = "/api/iot/*")
public class IotApi extends HttpServlet {

    private static final Logger LOG = Logger.getLogger(IotApi.class.getName());
    private static final LimiteTaxa LIMITE = new LimiteTaxa(240, 60_000L);   // 4 leituras/s por IP
    private static final int MAX_CORPO = 4 * 1024;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        tratar(req, resp, "GET");
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        tratar(req, resp, "POST");
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        tratar(req, resp, "PUT");
    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        tratar(req, resp, "DELETE");
    }

    private void tratar(HttpServletRequest req, HttpServletResponse resp, String metodo) throws IOException {
        resp.setHeader("Cache-Control", "no-store");
        try {
            Servicos s = Servicos.de(getServletContext());
            if (s == null) throw new ErroNegocio(503, "Serviço indisponível.");
            String chave = s.config.iotChave();
            if (chave == null || chave.isBlank()) throw new ErroNegocio(503, "A integração IoT está desligada (defina iot.chave).");
            if (!LIMITE.permitir(req.getRemoteAddr())) throw new ErroNegocio(429, "Leituras demais. Espere um pouco.");
            String recebida = req.getHeader("X-Sensor-Key");
            if (recebida == null || !MessageDigest.isEqual(recebida.getBytes(StandardCharsets.UTF_8), chave.getBytes(StandardCharsets.UTF_8)))
                throw new ErroNegocio(401, "Chave do sensor em falta ou inválida.");

            String[] p = partes(req.getPathInfo());
            Object r;
            if (p.length == 1 && "sensores".equals(p[0])) {
                exigir(metodo, "GET");
                r = s.sensores.listar();
            } else if (p.length == 3 && "sensores".equals(p[0]) && "leituras".equals(p[2])) {
                exigir(metodo, "GET");
                r = s.sensores.leituras(p[1], inteiro(req.getParameter("limite"), 20));
            } else if (p.length == 1 && "leituras".equals(p[0])) {
                exigir(metodo, "POST");
                Map<String, Object> d = corpo(req);
                r = s.sensores.registrar(d.get("sensor") instanceof String t ? t : null, d.get("valor"), d.get("lote"));
            } else {
                throw ErroNegocio.naoEncontrado("Recurso não encontrado.");
            }
            responder(resp, 200, r);
        } catch (ErroNegocio e) {
            responder(resp, e.getStatus(), Json.obj("erro", e.getMessage()));
        } catch (Banco.ErroBanco e) {
            LOG.log(Level.SEVERE, "Erro de banco em " + req.getRequestURI(), e);
            responder(resp, 503, Json.obj("erro", "Não foi possível falar com o banco de dados agora."));
        } catch (IllegalArgumentException e) {
            responder(resp, 400, Json.obj("erro", "JSON inválido."));
        } catch (RuntimeException e) {
            LOG.log(Level.SEVERE, "Erro inesperado em " + req.getRequestURI(), e);
            responder(resp, 500, Json.obj("erro", "Erro inesperado."));
        }
    }

    private static void exigir(String metodo, String esperado) {
        if (!esperado.equals(metodo)) throw new ErroNegocio(405, "Método não permitido para este recurso.");
    }

    private static String[] partes(String info) {
        if (info == null) return new String[0];
        String t = info.replaceAll("^/+|/+$", "");
        return t.isEmpty() ? new String[0] : t.split("/");
    }

    private static int inteiro(String v, int padrao) {
        try { return v == null ? padrao : Integer.parseInt(v.trim()); } catch (NumberFormatException e) { return padrao; }
    }

    private static Map<String, Object> corpo(HttpServletRequest req) throws IOException {
        String ct = req.getContentType();
        if (ct == null || !ct.toLowerCase().startsWith("application/json"))
            throw new ErroNegocio(415, "Envie a leitura em JSON (Content-Type: application/json).");
        byte[] b;
        try (InputStream in = req.getInputStream()) { b = in.readNBytes(MAX_CORPO + 1); }
        if (b.length > MAX_CORPO) throw new ErroNegocio(413, "Leitura grande demais.");
        Map<String, Object> d = Json.lerObjeto(new String(b, StandardCharsets.UTF_8));
        if (d == null) throw new IllegalArgumentException("corpo vazio");
        return d;
    }

    private static void responder(HttpServletResponse resp, int status, Object corpo) throws IOException {
        if (resp.isCommitted()) return;
        resp.setStatus(status);
        resp.setContentType("application/json;charset=UTF-8");
        byte[] b = Json.escrever(corpo).getBytes(StandardCharsets.UTF_8);
        resp.setContentLength(b.length);
        try (OutputStream o = resp.getOutputStream()) { o.write(b); }
    }
}
