package br.com.ongsave.service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import br.com.ongsave.dao.PlataformaDAO;
import br.com.ongsave.db.Banco;
import br.com.ongsave.model.ErroNegocio;
import br.com.ongsave.model.Usuario;
import br.com.ongsave.util.Json;

/**
 * Parâmetros globais editados pelo administrador (raio, fretes, planos, metodologia ESG, regras de risco).
 * Guardados como JSONB numa linha única e mantidos em cache por 30 s.
 */
public class PlataformaService {

    private static final long CACHE_MS = 30_000;
    private static final List<String> FRETES = List.of("carro", "camionete", "van", "caminhao");
    private static final List<String> PLANOS = List.of("pequena", "media", "grande");

    private final AuditoriaService auditoria;
    private final PlataformaDAO dao = new PlataformaDAO();
    private volatile Map<String, Object> cache;
    private volatile long lidoEm;

    public PlataformaService(AuditoriaService auditoria) {
        this.auditoria = auditoria;
    }

    /** Cópia defensiva dos parâmetros atuais. */
    public Map<String, Object> config() {
        Map<String, Object> c = cache;
        if (c == null || System.currentTimeMillis() - lidoEm > CACHE_MS) {
            String json = Banco.ler(dao::lerDados);
            c = Json.lerObjeto(json == null ? "{}" : json);
            cache = c;
            lidoEm = System.currentTimeMillis();
        }
        return Json.lerObjeto(Json.escrever(c));
    }

    public double raioKm() { return num(config(), "raioKm", 15); }

    public double tolPesoPct() { return num(sub(config(), "regras"), "tolPesoPct", 5); }

    public double freteDoLote(String veiculoLote) {
        String chave = switch (veiculoLote) { case "cam" -> "caminhao"; case "van" -> "van"; default -> "carro"; };
        return num(sub(config(), "fretes"), chave, 0);
    }

    public Map<String, Object> plano(String chave) {
        Map<String, Object> p = sub(sub(config(), "planos"), chave);
        if (p.isEmpty()) throw ErroNegocio.invalido("Plano desconhecido.");
        return p;
    }

    public double co2PorKg() { return num(sub(config(), "metodologia"), "co2PorKg", 1.69); }

    /**
     * Substitui os parâmetros depois de validar cada campo.
     * Campos desconhecidos são ignorados; faltantes mantêm o valor atual.
     */
    public Map<String, Object> atualizar(Map<String, Object> novo, Usuario admin) {
        Map<String, Object> atual = config();

        if (novo.containsKey("raioKm")) atual.put("raioKm", faixa(novo, "raioKm", 1, 100, "Raio máximo"));
        if (novo.containsKey("reservaPct")) atual.put("reservaPct", faixa(novo, "reservaPct", 1, 100, "Percentual da reserva"));

        if (novo.get("fretes") instanceof Map<?, ?> f) {
            Map<String, Object> fretes = sub(atual, "fretes");
            for (String k : FRETES) if (f.containsKey(k)) fretes.put(k, faixa(cast(f), k, 1, 10_000, "Frete " + k));
            atual.put("fretes", fretes);
        }
        if (novo.get("planos") instanceof Map<?, ?> ps) {
            Map<String, Object> planos = sub(atual, "planos");
            for (String k : PLANOS) {
                if (!(ps.get(k) instanceof Map<?, ?> p)) continue;
                Map<String, Object> destino = sub(planos, k);
                Map<String, Object> origem = cast(p);
                if (origem.containsKey("preco")) destino.put("preco", faixa(origem, "preco", 1, 1_000_000, "Preço do plano"));
                if (origem.containsKey("franquia")) destino.put("franquia", Math.round(faixa(origem, "franquia", 1, 10_000, "Franquia")));
                if (origem.containsKey("taxaExtra")) destino.put("taxaExtra", faixa(origem, "taxaExtra", 0, 100_000, "Taxa extra"));
                planos.put(k, destino);
            }
            atual.put("planos", planos);
        }
        if (novo.get("metodologia") instanceof Map<?, ?> m) {
            Map<String, Object> met = sub(atual, "metodologia");
            Map<String, Object> origem = cast(m);
            if (origem.containsKey("co2PorKg")) met.put("co2PorKg", faixa(origem, "co2PorKg", 0, 100, "Fator de CO₂"));
            if (origem.containsKey("refeicoesPorKg")) met.put("refeicoesPorKg", faixa(origem, "refeicoesPorKg", 0, 100, "Refeições por kg"));
            if (origem.containsKey("fonte")) {
                String fonte = String.valueOf(origem.get("fonte") == null ? "" : origem.get("fonte")).trim();
                if (fonte.length() > 300) throw ErroNegocio.invalido("A fonte da metodologia passa de 300 caracteres.");
                met.put("fonte", fonte);
            }
            atual.put("metodologia", met);
        }
        if (novo.get("regras") instanceof Map<?, ?> r) {
            Map<String, Object> regras = sub(atual, "regras");
            Map<String, Object> origem = cast(r);
            if (origem.containsKey("rejeicaoPct")) regras.put("rejeicaoPct", faixa(origem, "rejeicaoPct", 1, 100, "Limite de rejeição"));
            if (origem.containsKey("minEntregas")) regras.put("minEntregas", Math.round(faixa(origem, "minEntregas", 1, 10_000, "Mínimo de entregas")));
            if (origem.containsKey("tolPesoPct")) regras.put("tolPesoPct", faixa(origem, "tolPesoPct", 0, 50, "Tolerância de peso"));
            if (origem.containsKey("concentracaoPct")) regras.put("concentracaoPct", faixa(origem, "concentracaoPct", 10, 100, "Limite de concentração"));
            atual.put("regras", regras);
        }

        String json = Json.escrever(atual);
        Banco.transacao(c -> {
            dao.gravarDados(c, json);
            auditoria.registrar(c, admin, "Alterou parâmetros", "Plataforma", null,
                    "Raio " + fmt(num(atual, "raioKm", 0)) + " km, reserva " + fmt(num(atual, "reservaPct", 0)) + "%, planos, fretes e regras");
            return null;
        });
        cache = null;
        return config();
    }

    /* ---------- utilitários ---------- */

    @SuppressWarnings("unchecked")
    static Map<String, Object> sub(Map<String, Object> m, String k) {
        Object v = m.get(k);
        return v instanceof Map ? new LinkedHashMap<>((Map<String, Object>) v) : new LinkedHashMap<>();
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> cast(Map<?, ?> m) { return (Map<String, Object>) m; }

    static double num(Map<String, Object> m, String k, double padrao) {
        Object v = m.get(k);
        if (v instanceof Number n) return n.doubleValue();
        if (v instanceof String s) try { return Double.parseDouble(s); } catch (NumberFormatException e) { return padrao; }
        return padrao;
    }

    private static double faixa(Map<String, Object> m, String k, double min, double max, String rotulo) {
        double v = num(m, k, Double.NaN);
        if (Double.isNaN(v) || v < min || v > max)
            throw ErroNegocio.invalido(rotulo + " deve estar entre " + fmt(min) + " e " + fmt(max) + ".");
        return v;
    }

    private static String fmt(double v) {
        return v == Math.rint(v) ? String.valueOf((long) v) : String.valueOf(v);
    }
}
