package br.com.ongsave.service;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import br.com.ongsave.util.Json;

/**
 * Converte endereço em coordenadas com o Nominatim (OpenStreetMap).
 * Se falhar (sem rede, endereço não achado), devolve null e quem chamou mantém as coordenadas atuais.
 * Política de uso do Nominatim: 1 pedido/s e User-Agent identificável — suficiente para gravações de perfil.
 */
public class Geocodificador {

    private static final Logger LOG = Logger.getLogger(Geocodificador.class.getName());
    private final boolean ativo;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(4)).build();

    public Geocodificador(boolean ativo) {
        this.ativo = ativo;
    }

    /** @return {lat, lon} ou null */
    public double[] localizar(String rua, String numero, String bairro, String cidade, String uf, String cep) {
        if (!ativo) return null;
        String q = String.join(", ", naoVazios(rua + (numero == null || numero.isBlank() ? "" : " " + numero), bairro, cidade, uf, "Brasil"));
        double[] r = consultar(q);
        if (r == null && cep != null && !cep.isBlank()) r = consultar(cep + ", Brasil");
        return r;
    }

    private double[] consultar(String q) {
        try {
            HttpRequest req = HttpRequest.newBuilder(URI.create("https://nominatim.openstreetmap.org/search?format=json&limit=1&countrycodes=br&q="
                            + URLEncoder.encode(q, StandardCharsets.UTF_8)))
                    .timeout(Duration.ofSeconds(6))
                    .header("User-Agent", "OngSave/1.0 (plataforma de doacao de alimentos)")
                    .header("Accept-Language", "pt-BR")
                    .GET().build();
            HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() != 200) return null;
            Object v = Json.ler(resp.body());
            if (v instanceof List<?> l && !l.isEmpty() && l.get(0) instanceof Map<?, ?> m) {
                return new double[] { Double.parseDouble(String.valueOf(m.get("lat"))), Double.parseDouble(String.valueOf(m.get("lon"))) };
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            LOG.log(Level.INFO, "Geocodificação indisponível: {0}", e.getMessage());
        }
        return null;
    }

    private static List<String> naoVazios(String... s) {
        return java.util.Arrays.stream(s).filter(x -> x != null && !x.isBlank()).toList();
    }
}
