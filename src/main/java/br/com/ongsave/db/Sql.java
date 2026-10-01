package br.com.ongsave.db;

import java.math.BigDecimal;
import java.sql.Array;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Time;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Atalhos JDBC. Sempre com PreparedStatement (parâmetros ligados: sem SQL injection).
 *
 * As linhas são devolvidas como {@code Map<rótulo, valor>} já prontas para JSON:
 * use aliases entre aspas no SQL ({@code AS "pesoKg"}) para escolher os nomes dos campos.
 * Conversões: timestamp -> texto ISO-8601 · date -> "yyyy-MM-dd" · time -> "HH:mm" · array -> List.
 */
public final class Sql {
    private Sql() {}

    public static List<Map<String, Object>> lista(Connection c, String sql, Object... params) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ligar(ps, params);
            try (ResultSet rs = ps.executeQuery()) {
                ResultSetMetaData md = rs.getMetaData();
                int n = md.getColumnCount();
                List<Map<String, Object>> linhas = new ArrayList<>();
                while (rs.next()) {
                    Map<String, Object> m = new LinkedHashMap<>(n * 2);
                    for (int i = 1; i <= n; i++) m.put(md.getColumnLabel(i), converter(rs.getObject(i)));
                    linhas.add(m);
                }
                return linhas;
            }
        }
    }

    /** Primeira linha ou {@code null}. */
    public static Map<String, Object> linha(Connection c, String sql, Object... params) throws SQLException {
        List<Map<String, Object>> l = lista(c, sql, params);
        return l.isEmpty() ? null : l.get(0);
    }

    /** Valor da primeira coluna da primeira linha, ou {@code null}. */
    public static Object valor(Connection c, String sql, Object... params) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ligar(ps, params);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getObject(1) : null;
            }
        }
    }

    public static long numero(Connection c, String sql, Object... params) throws SQLException {
        Object v = valor(c, sql, params);
        return v == null ? 0 : ((Number) v).longValue();
    }

    public static double decimal(Connection c, String sql, Object... params) throws SQLException {
        Object v = valor(c, sql, params);
        return v == null ? 0 : ((Number) v).doubleValue();
    }

    public static int executar(Connection c, String sql, Object... params) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ligar(ps, params);
            return ps.executeUpdate();
        }
    }

    /** INSERT ... RETURNING id. */
    public static long inserir(Connection c, String sql, Object... params) throws SQLException {
        Object id = valor(c, sql + (sql.toUpperCase().contains("RETURNING") ? "" : " RETURNING id"), params);
        return ((Number) id).longValue();
    }

    public static byte[] bytes(Connection c, String sql, Object... params) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ligar(ps, params);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getBytes(1) : null;
            }
        }
    }

    /** Executa um script inteiro (várias instruções) — usado pelas migrações. */
    public static void script(Connection c, String sql) throws SQLException {
        try (Statement st = c.createStatement()) {
            st.execute(sql);
        }
    }

    public static Array arrayTexto(Connection c, List<String> itens) throws SQLException {
        return c.createArrayOf("text", itens.toArray());
    }

    private static void ligar(PreparedStatement ps, Object[] params) throws SQLException {
        for (int i = 0; i < params.length; i++) {
            Object p = params[i];
            int pos = i + 1;
            if (p == null) ps.setNull(pos, Types.NULL);
            else if (p instanceof Instant t) ps.setTimestamp(pos, Timestamp.from(t));
            else if (p instanceof LocalDate d) ps.setDate(pos, Date.valueOf(d));
            else if (p instanceof LocalTime t) ps.setTime(pos, Time.valueOf(t));
            else if (p instanceof byte[] b) ps.setBytes(pos, b);
            else if (p instanceof Array a) ps.setArray(pos, a);
            else if (p instanceof Enum<?> e) ps.setString(pos, e.name());
            else ps.setObject(pos, p);
        }
    }

    private static Object converter(Object v) throws SQLException {
        if (v == null) return null;
        if (v instanceof Timestamp t) return t.toInstant().toString();
        if (v instanceof Date d) return d.toLocalDate().toString();
        if (v instanceof Time t) return t.toLocalTime().toString().substring(0, 5);
        if (v instanceof java.time.OffsetDateTime o) return o.toInstant().toString();
        if (v instanceof Array a) {
            try { return Arrays.asList((Object[]) a.getArray()); }
            finally { a.free(); }
        }
        if (v instanceof BigDecimal || v instanceof Number || v instanceof String || v instanceof Boolean) return v;
        if (v instanceof byte[]) return null;              // binários nunca vão para JSON
        return v.toString();                              // PGobject (jsonb, etc.)
    }

    /* ---------- Leitura de mapas ---------- */

    public static double d(Map<String, Object> m, String k) {
        Object v = m.get(k);
        return v == null ? Double.NaN : ((Number) v).doubleValue();
    }

    public static long l(Map<String, Object> m, String k) {
        Object v = m.get(k);
        return v == null ? 0 : ((Number) v).longValue();
    }

    public static String s(Map<String, Object> m, String k) {
        Object v = m.get(k);
        return v == null ? null : v.toString();
    }
}
