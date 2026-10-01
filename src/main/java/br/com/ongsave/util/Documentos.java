package br.com.ongsave.util;

/** Validação de CPF, CNPJ, placa e telefone brasileiros (dígitos verificadores incluídos). */
public final class Documentos {
    private Documentos() {}

    public static String digitos(String s) {
        return s == null ? "" : s.replaceAll("\\D", "");
    }

    public static boolean cpfValido(String cpf) {
        String d = digitos(cpf);
        if (d.length() != 11 || d.chars().distinct().count() == 1) return false;
        return dv(d, 9, 10) == d.charAt(9) - '0' && dv(d, 10, 11) == d.charAt(10) - '0';
    }

    private static int dv(String d, int n, int pesoInicial) {
        int soma = 0;
        for (int i = 0; i < n; i++) soma += (d.charAt(i) - '0') * (pesoInicial - i);
        int r = (soma * 10) % 11;
        return r == 10 ? 0 : r;
    }

    public static boolean cnpjValido(String cnpj) {
        String d = digitos(cnpj);
        if (d.length() != 14 || d.chars().distinct().count() == 1) return false;
        int[] p1 = { 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2 };
        int[] p2 = { 6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2 };
        return dvCnpj(d, p1) == d.charAt(12) - '0' && dvCnpj(d, p2) == d.charAt(13) - '0';
    }

    private static int dvCnpj(String d, int[] pesos) {
        int soma = 0;
        for (int i = 0; i < pesos.length; i++) soma += (d.charAt(i) - '0') * pesos[i];
        int r = soma % 11;
        return r < 2 ? 0 : 11 - r;
    }

    public static String formatarCpf(String cpf) {
        String d = digitos(cpf);
        return d.length() == 11 ? d.substring(0, 3) + "." + d.substring(3, 6) + "." + d.substring(6, 9) + "-" + d.substring(9) : cpf;
    }

    public static String formatarCnpj(String cnpj) {
        String d = digitos(cnpj);
        return d.length() == 14 ? d.substring(0, 2) + "." + d.substring(2, 5) + "." + d.substring(5, 8) + "/" + d.substring(8, 12) + "-" + d.substring(12) : cnpj;
    }

    /** Placa antiga (ABC1234) ou Mercosul (ABC1D23). Devolve normalizada ou null. */
    public static String placa(String s) {
        String p = s == null ? "" : s.replaceAll("[\\s-]", "").toUpperCase(java.util.Locale.ROOT);
        return p.matches("[A-Z]{3}[0-9][A-Z0-9][0-9]{2}") ? p : null;
    }

    public static boolean telefoneValido(String tel) {
        int n = digitos(tel).length();
        return n == 10 || n == 11;
    }
}
