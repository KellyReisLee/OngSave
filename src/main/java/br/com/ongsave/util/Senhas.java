package br.com.ongsave.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.security.spec.KeySpec;
import java.util.Base64;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/**
 * Hash de senhas com PBKDF2-HMAC-SHA256 (JDK puro, sem dependências).
 * Formato guardado: {@code pbkdf2_sha256$<iteracoes>$<sal base64>$<hash base64>}.
 */
public final class Senhas {
    private static final String ALGORITMO = "pbkdf2_sha256";
    private static final int ITERACOES = 120_000;
    private static final SecureRandom RANDOM = new SecureRandom();
    /** Hash descartável usado quando o e-mail não existe (tempo de resposta igual). */
    private static final String FICTICIO = gerar("senha-ficticia-para-tempo-constante");

    private Senhas() {}

    public static String gerar(String senha) {
        byte[] sal = new byte[16];
        RANDOM.nextBytes(sal);
        byte[] h = pbkdf2(senha, sal, ITERACOES);
        Base64.Encoder b64 = Base64.getEncoder();
        return ALGORITMO + "$" + ITERACOES + "$" + b64.encodeToString(sal) + "$" + b64.encodeToString(h);
    }

    public static boolean confere(String senha, String guardado) {
        if (senha == null) return false;
        if (guardado == null) { confere(senha, FICTICIO); return false; }
        String[] p = guardado.split("\\$");
        if (p.length != 4 || !ALGORITMO.equals(p[0])) return false;
        try {
            int it = Integer.parseInt(p[1]);
            byte[] sal = Base64.getDecoder().decode(p[2]);
            byte[] esperado = Base64.getDecoder().decode(p[3]);
            return MessageDigest.isEqual(esperado, pbkdf2(senha, sal, it));
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    /** Simula o custo de uma verificação (para e-mails inexistentes). */
    public static void gastarTempo(String senha) {
        confere(senha == null ? "" : senha, FICTICIO);
    }

    private static byte[] pbkdf2(String senha, byte[] sal, int iteracoes) {
        try {
            KeySpec spec = new PBEKeySpec(senha.toCharArray(), sal, iteracoes, 256);
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
        } catch (Exception e) {
            throw new IllegalStateException("Falha ao calcular hash da senha", e);
        }
    }

    /** Comparação em tempo constante de códigos curtos (token, código de retirada). */
    public static boolean iguais(String a, String b) {
        if (a == null || b == null) return false;
        return MessageDigest.isEqual(a.getBytes(StandardCharsets.UTF_8), b.getBytes(StandardCharsets.UTF_8));
    }
}
