package br.com.ongsave.service;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.IsoFields;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import br.com.ongsave.util.Json;

/**
 * Agregações para os gráficos e KPIs dos painéis (30 dias, 90 dias, acumulado do ano).
 * Os dados chegam já filtrados do banco (entregas de uma empresa ou de uma ONG).
 */
final class Estatisticas {
    private Estatisticas() {}

    static final ZoneId FUSO = ZoneId.of("America/Sao_Paulo");
    private static final String[] MESES = { "Jan", "Fev", "Mar", "Abr", "Mai", "Jun", "Jul", "Ago", "Set", "Out", "Nov", "Dez" };

    /** Uma entrega: quando foi validada, quantos kg e com quem (empresa ou ONG, para contar parceiros). */
    record Ponto(Instant quando, double kg, long parceiro, String categoria) {}

    static String mes(int m) { return MESES[m - 1]; }

    /** Mapa {30:{...}, 90:{...}, 0:{...}} no formato usado pelos dashboards da empresa e da ONG. */
    static Map<String, Object> periodos(List<Ponto> pts) {
        Instant agora = Instant.now();
        ZonedDateTime hoje = ZonedDateTime.now(FUSO);
        Map<String, Object> r = new LinkedHashMap<>();

        // ----- 30 dias, por semana -----
        Instant ini30 = agora.minus(Duration.ofDays(28));
        double[] kgS = new double[4];
        long[] ltS = new long[4];
        for (Ponto p : pts) {
            if (p.quando().isBefore(ini30)) continue;
            int idx = 3 - (int) Math.min(3, Duration.between(p.quando(), agora).toDays() / 7);
            kgS[idx] += p.kg();
            ltS[idx]++;
        }
        r.put("30", bloco("Últimos 30 dias", "Por semana, últimos 30 dias", 30, pts, agora.minus(Duration.ofDays(30)), agora,
                agora.minus(Duration.ofDays(60)), List.of("Sem 1", "Sem 2", "Sem 3", "Sem 4"), kgS, ltS));

        // ----- 90 dias, por mês (3 meses) -----
        List<String> lab90 = new ArrayList<>();
        double[] kg90 = new double[3];
        long[] lt90 = new long[3];
        ZonedDateTime inicioMes = hoje.withDayOfMonth(1).toLocalDate().atStartOfDay(FUSO);
        for (int i = 2; i >= 0; i--) lab90.add(mes(inicioMes.minusMonths(i).getMonthValue()));
        Instant ini90 = inicioMes.minusMonths(2).toInstant();
        for (Ponto p : pts) {
            if (p.quando().isBefore(ini90)) continue;
            ZonedDateTime z = p.quando().atZone(FUSO);
            int idx = 2 - (int) ((inicioMes.getYear() * 12L + inicioMes.getMonthValue()) - (z.getYear() * 12L + z.getMonthValue()));
            if (idx < 0 || idx > 2) continue;
            kg90[idx] += p.kg();
            lt90[idx]++;
        }
        r.put("90", bloco("Últimos 90 dias", "Por mês, últimos 90 dias", 90, pts, agora.minus(Duration.ofDays(90)), agora,
                agora.minus(Duration.ofDays(180)), lab90, kg90, lt90));

        // ----- Acumulado do ano, por mês -----
        int meses = hoje.getMonthValue();
        List<String> labAno = new ArrayList<>();
        double[] kgA = new double[meses];
        long[] ltA = new long[meses];
        for (int m = 1; m <= meses; m++) labAno.add(mes(m));
        Instant iniAno = LocalDate.of(hoje.getYear(), 1, 1).atStartOfDay(FUSO).toInstant();
        for (Ponto p : pts) {
            if (p.quando().isBefore(iniAno)) continue;
            int m = p.quando().atZone(FUSO).getMonthValue();
            kgA[m - 1] += p.kg();
            ltA[m - 1]++;
        }
        Instant iniAnoAnt = LocalDate.of(hoje.getYear() - 1, 1, 1).atStartOfDay(FUSO).toInstant();
        Instant mesmoDiaAnoAnt = hoje.minusYears(1).toInstant();
        int dias = (int) Math.max(1, Duration.between(iniAno, agora).toDays());
        Map<String, Object> ano = bloco("Acumulado de " + hoje.getYear(), "Por mês, acumulado de " + hoje.getYear(), dias, pts,
                iniAno, agora, null, labAno, kgA, ltA);
        ano.put("delta", delta(soma(pts, iniAno, agora), soma(pts, iniAnoAnt, mesmoDiaAnoAnt)));
        r.put("0", ano);
        return r;
    }

    private static Map<String, Object> bloco(String nome, String sub, int dias, List<Ponto> pts, Instant ini, Instant fim,
                                             Instant iniAnterior, List<String> labels, double[] kg, long[] lotes) {
        double total = 0;
        long n = 0;
        Set<Long> parceiros = new HashSet<>();
        for (Ponto p : pts) {
            if (p.quando().isBefore(ini) || p.quando().isAfter(fim)) continue;
            total += p.kg();
            n++;
            parceiros.add(p.parceiro());
        }
        List<Double> kgL = new ArrayList<>();
        for (double v : kg) kgL.add(Math.round(v * 10) / 10.0);
        List<Long> ltL = new ArrayList<>();
        for (long v : lotes) ltL.add(v);
        Map<String, Object> m = Json.obj("nome", nome, "sub", sub, "dias", dias, "kg", Math.round(total * 10) / 10.0, "lotes", n,
                "empresas", parceiros.size(), "ongs", parceiros.size(), "labels", labels, "kgS", kgL, "loteS", ltL);
        m.put("delta", iniAnterior == null ? "—" : delta(total, soma(pts, iniAnterior, ini)));
        return m;
    }

    private static double soma(List<Ponto> pts, Instant ini, Instant fim) {
        double s = 0;
        for (Ponto p : pts) if (!p.quando().isBefore(ini) && p.quando().isBefore(fim)) s += p.kg();
        return s;
    }

    static String delta(double atual, double anterior) {
        if (anterior <= 0) return atual > 0 ? "novo" : "—";
        long pct = Math.round((atual - anterior) / anterior * 100);
        return (pct >= 0 ? "+" : "") + pct + "%";
    }

    /** kg por categoria (para os gráficos de rosca). */
    static Map<String, Object> categorias(List<Ponto> pts) {
        Map<String, Double> soma = new LinkedHashMap<>();
        for (Ponto p : pts) soma.merge(curta(p.categoria()), p.kg(), Double::sum);
        List<String> labels = new ArrayList<>();
        List<Long> dados = new ArrayList<>();
        soma.entrySet().stream().sorted((a, b) -> Double.compare(b.getValue(), a.getValue())).forEach(e -> {
            labels.add(e.getKey());
            dados.add(Math.round(e.getValue()));
        });
        return Json.obj("labels", labels, "data", dados);
    }

    static String curta(String categoria) {
        if (categoria == null) return "Outros";
        int i = categoria.indexOf(" /");
        return i > 0 ? categoria.substring(0, i) : categoria;
    }

    /** Relatório ESG: CO₂ acumulado por trimestre (5 trimestres) e três períodos com início/fim. */
    static Map<String, Object> esg(List<Ponto> pts) {
        ZonedDateTime hoje = ZonedDateTime.now(FUSO);
        int q = hoje.get(IsoFields.QUARTER_OF_YEAR);
        int ano = hoje.getYear();
        List<String> labels = new ArrayList<>();
        List<Double> kgAc = new ArrayList<>();
        for (int i = 4; i >= 0; i--) {
            int qq = q - i, aa = ano;
            while (qq <= 0) { qq += 4; aa--; }
            Instant fimQ = LocalDate.of(aa, (qq - 1) * 3 + 1, 1).plusMonths(3).atStartOfDay(FUSO).toInstant();
            labels.add("Q" + qq + " " + aa);
            double s = 0;
            for (Ponto p : pts) if (p.quando().isBefore(fimQ)) s += p.kg();
            kgAc.add(Math.round(s * 10) / 10.0);
        }
        Instant iniQ = LocalDate.of(ano, (q - 1) * 3 + 1, 1).atStartOfDay(FUSO).toInstant();
        Instant iniAnoAnt = LocalDate.of(ano - 1, 1, 1).atStartOfDay(FUSO).toInstant();
        Instant iniAno = LocalDate.of(ano, 1, 1).atStartOfDay(FUSO).toInstant();
        Instant agora = Instant.now();
        Map<String, Object> periodos = new LinkedHashMap<>();
        periodos.put("tudo", periodoEsg("Acumulado até Q" + q + " " + ano, pts, Instant.EPOCH, agora));
        periodos.put("2025", periodoEsg("Ano de " + (ano - 1), pts, iniAnoAnt, iniAno));
        periodos.put("q1", periodoEsg("Q" + q + " " + ano, pts, iniQ, agora));
        return Json.obj("trim", Json.obj("labels", labels, "kgAc", kgAc), "periodos", periodos, "categorias", categorias(pts));
    }

    private static Map<String, Object> periodoEsg(String nome, List<Ponto> pts, Instant ini, Instant fim) {
        double kg = 0;
        long n = 0;
        Set<Long> ongs = new HashSet<>();
        for (Ponto p : pts) {
            if (p.quando().isBefore(ini) || !p.quando().isBefore(fim)) continue;
            kg += p.kg();
            n++;
            ongs.add(p.parceiro());
        }
        return Json.obj("nome", nome, "kg", Math.round(kg * 10) / 10.0, "lotes", n, "ongs", ongs.size(),
                "inicio", ini.toEpochMilli(), "fim", fim.toEpochMilli());
    }
}
