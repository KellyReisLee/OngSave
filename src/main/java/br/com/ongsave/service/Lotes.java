package br.com.ongsave.service;

import java.util.Map;

import br.com.ongsave.db.Sql;
import br.com.ongsave.util.Geo;
import br.com.ongsave.util.Json;

/**
 * Funções sobre a "vista de lote" devolvida pelos métodos {@code detalhes*} do {@link br.com.ongsave.dao.LoteDAO}:
 * veículo, progresso, retirada, token e conferência no formato que o JavaScript de cada painel espera.
 */
final class Lotes {
    private Lotes() {}

    /** Rótulo mostrado à empresa e à ONG. */
    static String veiculoEmpresa(Map<String, Object> l) {
        String base = switch (Sql.s(l, "veicKey")) { case "cam" -> "Caminhão de Carga"; case "van" -> "Van"; default -> "Carro Utilitário"; };
        return "amb".equals(l.get("cons")) ? base : base + " (refrigerado)";
    }

    /** Nome do veículo como o motorista o conhece (mesma lista do perfil do motorista). */
    static String veiculoMotorista(String veicKey) {
        return switch (veicKey) { case "cam" -> "Caminhão"; case "van" -> "Van"; default -> "Carro Económico"; };
    }

    static int rankMotorista(String veiculo) {
        return switch (veiculo == null ? "" : veiculo) {
            case "Caminhão" -> 4; case "Van" -> 3; case "Camionete" -> 2; default -> 1;
        };
    }

    static int rankLote(String veicKey) {
        return switch (veicKey) { case "cam" -> 4; case "van" -> 3; default -> 1; };
    }

    /* ---------- Posição e progresso ---------- */

    static boolean temPosicao(Map<String, Object> l) { return l.get("posLat") != null; }

    /** Progresso 0..1 do trajeto empresa -> ONG, pela última posição recebida do motorista. */
    static double progresso(Map<String, Object> l) {
        String estado = Sql.s(l, "estado");
        if ("entregue".equals(estado)) return 1;
        if (!"transito".equals(estado)) return 0;
        if (!temPosicao(l)) return 0.05;
        double total = Geo.metros(Sql.d(l, "empLat"), Sql.d(l, "empLon"), Sql.d(l, "ongLat"), Sql.d(l, "ongLon"));
        double resta = Geo.metros(Sql.d(l, "posLat"), Sql.d(l, "posLon"), Sql.d(l, "ongLat"), Sql.d(l, "ongLon"));
        if (total < 1) return 1;
        return Math.max(0, Math.min(1, 1 - resta / total));
    }

    /** Distância (m) da última posição do motorista até a ONG, ou NaN sem posição. */
    static double metrosAteOng(Map<String, Object> l) {
        if (!temPosicao(l)) return Double.NaN;
        return Geo.metros(Sql.d(l, "posLat"), Sql.d(l, "posLon"), Sql.d(l, "ongLat"), Sql.d(l, "ongLon"));
    }

    /* ---------- Blocos aninhados reutilizados ---------- */

    static Map<String, Object> retirada(Map<String, Object> l) {
        if (l.get("retCodigo") == null) return null;
        return Json.obj("codigo", l.get("retCodigo"), "kg", l.get("retKg"), "temp", l.get("retTemp"),
                "obs", l.get("retObs") == null ? "" : l.get("retObs"), "em", l.get("retMs"));
    }

    static Map<String, Object> conferencia(Map<String, Object> l) {
        if (l.get("confKg") == null) return null;
        return Json.obj("kg", l.get("confKg"), "cond", l.get("confCond"), "obs", l.get("confObs") == null ? "" : l.get("confObs"),
                "div", l.get("confDiv") == null ? 0 : l.get("confDiv"), "retido", l.get("freteRetido"));
    }

    static Map<String, Object> token(Map<String, Object> l) {
        if (l.get("tokCodigo") == null) return null;
        return Json.obj("codigo", l.get("tokCodigo"), "em", l.get("tokMs"), "expira", l.get("tokExpira"), "usado", l.get("tokUsado"));
    }

    static double horas(Map<String, Object> l) {
        Object h = l.get("h");
        return h == null ? 0 : Math.round(((Number) h).doubleValue() * 100) / 100.0;
    }
}
