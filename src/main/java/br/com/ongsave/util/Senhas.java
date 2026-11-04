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
 *
 * <p>600 000 iterações = recomendação da OWASP (Password Storage Cheat Sheet) para PBKDF2-HMAC-SHA256.
 * Hashes antigos, com menos iterações, continuam a ser aceites e são refeitos no login seguinte
 * ({@link #precisaAtualizar}).</p>
 */
public final class Senhas {
    private static final String ALGORITMO = "pbkdf2_sha256";
    private static final int ITERACOES = 600_000;
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

    /** true se o hash foi gerado com menos iterações do que as atuais (deve ser refeito no próximo login). */
    public static boolean precisaAtualizar(String guardado) {
        if (guardado == null) return false;
        String[] p = guardado.split("\\$");
        if (p.length != 4 || !ALGORITMO.equals(p[0])) return true;
        try { return Integer.parseInt(p[1]) < ITERACOES; } catch (NumberFormatException e) { return true; }
    }

    /** SHA-256 em hexadecimal (para guardar tokens aleatórios: não precisam de sal nem de hash lento). */
    public static String sha256(String texto) {
        try {
            byte[] h = MessageDigest.getInstance("SHA-256").digest(texto.getBytes(StandardCharsets.UTF_8));
            return java.util.HexFormat.of().formatHex(h);
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    /** Senhas das listas de senhas mais usadas/vazadas, ou um único caractere repetido. */
    private static final java.util.Set<String> COMUNS = java.util.Set.of(
            "12345678", "123456789", "1234567890", "12341234", "11111111", "00000000", "87654321", "123123123",
            "password", "password1", "password123", "senha123", "senha1234", "senhasenha", "qwerty123", "qwertyuiop",
            "abc12345", "abcd1234", "iloveyou", "admin123", "administrador", "mudar123", "brasil123", "portugal",
            "asdfghjk", "1q2w3e4r", "1q2w3e4r5t", "zaq12wsx", "princesa", "flamengo", "corinthians", "palmeiras");

    public static boolean fraca(String senha) {
        if (senha == null) return true;
        String s = senha.toLowerCase(java.util.Locale.ROOT);
        return COMUNS.contains(s) || s.chars().distinct().count() <= 2;
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
