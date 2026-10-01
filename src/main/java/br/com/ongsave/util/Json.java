package br.com.ongsave.util;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.TemporalAccessor;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * JSON mínimo e sem dependências (serialização + leitura).
 *
 * A saída escapa {@code < > & ' U+2028 U+2029}, por isso pode ser embutida com segurança
 * dentro de {@code <script type="application/json">} numa JSP.
 */
public final class Json {

    private Json() {}

    /** Valor que já é JSON válido (ex.: coluna jsonb) e deve ser escrito sem aspas. */
    public record Bruto(String json) {}

    /** Atalho para montar objetos: {@code Json.obj("id", 1, "nome", "x")}. */
    public static Map<String, Object> obj(Object... chavesValores) {
        if (chavesValores.length % 2 != 0) throw new IllegalArgumentException("Pares chave/valor incompletos");
        Map<String, Object> m = new LinkedHashMap<>();
        for (int i = 0; i < chavesValores.length; i += 2) m.put(String.valueOf(chavesValores[i]), chavesValores[i + 1]);
        return m;
    }

    /* ============================ Escrita ============================ */

    public static String escrever(Object valor) {
        StringBuilder sb = new StringBuilder(256);
        escrever(sb, valor);
        return sb.toString();
    }

    @SuppressWarnings("unchecked")
    private static void escrever(StringBuilder sb, Object v) {
        if (v == null) { sb.append("null"); return; }
        if (v instanceof Bruto b) { sb.append(b.json() == null ? "null" : b.json()); return; }
        if (v instanceof String s) { texto(sb, s); return; }
        if (v instanceof Boolean) { sb.append(v); return; }
        if (v instanceof BigDecimal d) { sb.append(d.stripTrailingZeros().toPlainString()); return; }
        if (v instanceof Double d) { sb.append(d.isNaN() || d.isInfinite() ? "null" : numero(d)); return; }
        if (v instanceof Float f) { sb.append(f.isNaN() || f.isInfinite() ? "null" : numero(f.doubleValue())); return; }
        if (v instanceof Number) { sb.append(v); return; }
        if (v instanceof Instant || v instanceof TemporalAccessor) { texto(sb, v.toString()); return; }
        if (v instanceof Map<?, ?> m) {
            sb.append('{');
            boolean primeiro = true;
            for (Map.Entry<?, ?> e : ((Map<Object, Object>) m).entrySet()) {
                if (!primeiro) sb.append(',');
                primeiro = false;
                texto(sb, String.valueOf(e.getKey()));
                sb.append(':');
                escrever(sb, e.getValue());
            }
            sb.append('}');
            return;
        }
        if (v instanceof Collection<?> c) {
            sb.append('[');
            boolean primeiro = true;
            for (Object o : c) {
                if (!primeiro) sb.append(',');
                primeiro = false;
                escrever(sb, o);
            }
            sb.append(']');
            return;
        }
        if (v instanceof Object[] arr) { escrever(sb, List.of(arr)); return; }
        texto(sb, v.toString());
    }

    private static String numero(double d) {
        return d == Math.rint(d) && Math.abs(d) < 1e15 ? String.valueOf((long) d) : String.valueOf(d);
    }

    private static void texto(StringBuilder sb, String s) {
        sb.append('"');
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                case '<' -> sb.append("\\u003c");
                case '>' -> sb.append("\\u003e");
                case '&' -> sb.append("\\u0026");
                case '\'' -> sb.append("\\u0027");
                case '\u2028' -> sb.append("\\u2028");
                case '\u2029' -> sb.append("\\u2029");
                default -> {
                    if (c < 0x20) sb.append(String.format("\\u%04x", (int) c));
                    else sb.append(c);
                }
            }
        }
        sb.append('"');
    }

    /* ============================ Leitura ============================ */

    /** Converte texto JSON em Map / List / String / Double / Boolean / null. */
    public static Object ler(String json) {
        if (json == null) throw new IllegalArgumentException("JSON vazio");
        Leitor l = new Leitor(json);
        Object v = l.valor(0);
        l.espacos();
        if (l.i != json.length()) throw l.erro("conteúdo depois do fim");
        return v;
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> lerObjeto(String json) {
        Object v = ler(json);
        if (!(v instanceof Map)) throw new IllegalArgumentException("Era esperado um objeto JSON");
        return (Map<String, Object>) v;
    }

    private static final class Leitor {
        private static final int PROFUNDIDADE_MAX = 32;
        private final String s;
        private int i;

        Leitor(String s) { this.s = s; }

        IllegalArgumentException erro(String msg) {
            return new IllegalArgumentException("JSON inválido na posição " + i + ": " + msg);
        }

        void espacos() {
            while (i < s.length() && Character.isWhitespace(s.charAt(i))) i++;
        }

        Object valor(int nivel) {
            if (nivel > PROFUNDIDADE_MAX) throw erro("aninhamento excessivo");
            espacos();
            if (i >= s.length()) throw erro("fim inesperado");
            char c = s.charAt(i);
            return switch (c) {
                case '{' -> objeto(nivel);
                case '[' -> lista(nivel);
                case '"' -> texto();
                case 't' -> literal("true", Boolean.TRUE);
                case 'f' -> literal("false", Boolean.FALSE);
                case 'n' -> literal("null", null);
                default -> numero();
            };
        }

        Map<String, Object> objeto(int nivel) {
            Map<String, Object> m = new LinkedHashMap<>();
            i++;
            espacos();
            if (i < s.length() && s.charAt(i) == '}') { i++; return m; }
            while (true) {
                espacos();
                if (i >= s.length() || s.charAt(i) != '"') throw erro("chave esperada");
                String k = texto();
                espacos();
                if (i >= s.length() || s.charAt(i) != ':') throw erro("':' esperado");
                i++;
                m.put(k, valor(nivel + 1));
                espacos();
                if (i >= s.length()) throw erro("fim inesperado");
                char c = s.charAt(i++);
                if (c == '}') return m;
                if (c != ',') throw erro("',' ou '}' esperado");
            }
        }

        List<Object> lista(int nivel) {
            List<Object> l = new ArrayList<>();
            i++;
            espacos();
            if (i < s.length() && s.charAt(i) == ']') { i++; return l; }
            while (true) {
                l.add(valor(nivel + 1));
                espacos();
                if (i >= s.length()) throw erro("fim inesperado");
                char c = s.charAt(i++);
                if (c == ']') return l;
                if (c != ',') throw erro("',' ou ']' esperado");
            }
        }

        String texto() {
            StringBuilder sb = new StringBuilder();
            i++;
            while (i < s.length()) {
                char c = s.charAt(i++);
                if (c == '"') return sb.toString();
                if (c == '\\') {
                    if (i >= s.length()) break;
                    char e = s.charAt(i++);
                    switch (e) {
                        case '"', '\\', '/' -> sb.append(e);
                        case 'b' -> sb.append('\b');
                        case 'f' -> sb.append('\f');
                        case 'n' -> sb.append('\n');
                        case 'r' -> sb.append('\r');
                        case 't' -> sb.append('\t');
                        case 'u' -> {
                            if (i + 4 > s.length()) throw erro("escape unicode incompleto");
                            sb.append((char) Integer.parseInt(s.substring(i, i + 4), 16));
                            i += 4;
                        }
                        default -> throw erro("escape inválido");
                    }
                } else sb.append(c);
            }
            throw erro("texto sem fim");
        }

        Object literal(String palavra, Object valor) {
            if (!s.startsWith(palavra, i)) throw erro("literal inválido");
            i += palavra.length();
            return valor;
        }

        Double numero() {
            int ini = i;
            while (i < s.length() && "+-0123456789.eE".indexOf(s.charAt(i)) >= 0) i++;
            if (ini == i) throw erro("valor inesperado");
            try {
                return Double.valueOf(s.substring(ini, i));
            } catch (NumberFormatException e) {
                throw erro("número inválido");
            }
        }
    }
}
