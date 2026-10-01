package br.com.ongsave.util;

/** Cálculos geográficos simples (haversine). */
public final class Geo {
    /** Centro de São Paulo: usado quando um endereço ainda não foi geocodificado. */
    public static final double LAT_PADRAO = -23.5505;
    public static final double LON_PADRAO = -46.6333;

    private Geo() {}

    public static double metros(double lat1, double lon1, double lat2, double lon2) {
        double r = Math.PI / 180;
        double dLat = (lat2 - lat1) * r, dLon = (lon2 - lon1) * r;
        double h = Math.pow(Math.sin(dLat / 2), 2)
                + Math.cos(lat1 * r) * Math.cos(lat2 * r) * Math.pow(Math.sin(dLon / 2), 2);
        return 12_742_000 * Math.asin(Math.sqrt(h));
    }

    public static double km(double lat1, double lon1, double lat2, double lon2) {
        return metros(lat1, lon1, lat2, lon2) / 1000.0;
    }

    public static boolean coordenadaValida(double lat, double lon) {
        return !Double.isNaN(lat) && !Double.isNaN(lon) && lat >= -90 && lat <= 90 && lon >= -180 && lon <= 180;
    }
}
