package br.com.ongsave.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.HexFormat;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

/**
 * Token anti-CSRF por sessão.
 * - Formulários: <input type="hidden" name="csrf" value="${sessionScope.csrfToken}">
 * - fetch() do motorista (nucleo.js): cabeçalho X-CSRF-Token lido de <meta name="csrf">
 */
public final class Csrf {
    public static final String ATRIBUTO = "csrfToken";
    private static final SecureRandom RANDOM = new SecureRandom();

    private Csrf() {}

    public static String garantir(HttpSession sessao) {
        String t = (String) sessao.getAttribute(ATRIBUTO);
        if (t == null) {
            byte[] b = new byte[32];
            RANDOM.nextBytes(b);
            t = HexFormat.of().formatHex(b);
            sessao.setAttribute(ATRIBUTO, t);
        }
        return t;
    }

    public static boolean valido(HttpServletRequest req) {
        HttpSession s = req.getSession(false);
        if (s == null) return false;
        String esperado = (String) s.getAttribute(ATRIBUTO);
        String recebido = req.getParameter("csrf");
        if (recebido == null) recebido = req.getHeader("X-CSRF-Token");
        return esperado != null && recebido != null && MessageDigest.isEqual(
                esperado.getBytes(StandardCharsets.UTF_8), recebido.getBytes(StandardCharsets.UTF_8));
    }
}
