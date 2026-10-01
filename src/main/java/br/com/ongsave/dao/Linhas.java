package br.com.ongsave.dao;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Conversão das linhas devolvidas por {@link br.com.ongsave.db.Sql} (mapas coluna → valor) para os tipos do model.
 * O Sql já entrega datas como texto ISO, números como Number e arrays como List.
 */
final class Linhas {
    private Linhas() {}

    static long l(Map<String, Object> m, String k) {
        Object v = m.get(k);
        return v == null ? 0 : ((Number) v).longValue();
    }

    static Long lng(Map<String, Object> m, String k) {
        Object v = m.get(k);
        return v == null ? null : ((Number) v).longValue();
    }

    static int i(Map<String, Object> m, String k) {
        Object v = m.get(k);
        return v == null ? 0 : ((Number) v).intValue();
    }

    static Integer inteiro(Map<String, Object> m, String k) {
        Object v = m.get(k);
        return v == null ? null : ((Number) v).intValue();
    }

    static double d(Map<String, Object> m, String k) {
        Object v = m.get(k);
        return v == null ? 0 : ((Number) v).doubleValue();
    }

    static Double dbl(Map<String, Object> m, String k) {
        Object v = m.get(k);
        return v == null ? null : ((Number) v).doubleValue();
    }

    static Float flt(Map<String, Object> m, String k) {
        Object v = m.get(k);
        return v == null ? null : ((Number) v).floatValue();
    }

    static String s(Map<String, Object> m, String k) {
        Object v = m.get(k);
        return v == null ? null : v.toString();
    }

    static boolean b(Map<String, Object> m, String k) {
        return Boolean.TRUE.equals(m.get(k));
    }

    static Instant instante(Map<String, Object> m, String k) {
        Object v = m.get(k);
        return v == null ? null : Instant.parse(v.toString());
    }

    static LocalDate data(Map<String, Object> m, String k) {
        Object v = m.get(k);
        return v == null ? null : LocalDate.parse(v.toString());
    }

    static LocalTime hora(Map<String, Object> m, String k) {
        Object v = m.get(k);
        return v == null ? null : LocalTime.parse(v.toString());
    }

    static List<String> textos(Map<String, Object> m, String k) {
        Object v = m.get(k);
        List<String> r = new ArrayList<>();
        if (v instanceof List<?> lista) for (Object o : lista) if (o != null) r.add(o.toString());
        return r;
    }
}
