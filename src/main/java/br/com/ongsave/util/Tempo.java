package br.com.ongsave.util;

import java.time.Duration;
import java.time.Instant;

/** Textos de tempo relativo ("agora", "há 5 min", "ontem") usados nas notificações. */
public final class Tempo {
    private Tempo() {}

    public static String relativo(Instant quando) {
        if (quando == null) return "";
        long s = Math.max(0, Duration.between(quando, Instant.now()).getSeconds());
        if (s < 60) return "agora";
        if (s < 3600) return "há " + (s / 60) + " min";
        if (s < 86_400) return "há " + (s / 3600) + " h";
        if (s < 2 * 86_400) return "ontem";
        return "há " + (s / 86_400) + " dias";
    }
}
