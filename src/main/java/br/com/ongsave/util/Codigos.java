package br.com.ongsave.util;

import java.security.SecureRandom;

/** Códigos curtos para humanos (sem 0/O, 1/I para evitar confusão ao ditar). */
public final class Codigos {
    private static final String ALFABETO = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    private Codigos() {}

    public static String gerar(int tamanho) {
        StringBuilder sb = new StringBuilder(tamanho);
        for (int i = 0; i < tamanho; i++) sb.append(ALFABETO.charAt(RANDOM.nextInt(ALFABETO.length())));
        return sb.toString();
    }

    /** Normaliza o que o utilizador digitou: maiúsculas, sem espaços nem hífens. */
    public static String normalizar(String s) {
        return s == null ? "" : s.replaceAll("[\\s-]", "").toUpperCase(java.util.Locale.ROOT);
    }
}
