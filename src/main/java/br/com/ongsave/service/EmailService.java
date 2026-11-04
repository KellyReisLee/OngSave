package br.com.ongsave.service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

import br.com.ongsave.util.Json;

/**
 * Envio de e-mails transacionais (recuperação de senha).
 *
 * <p>Com {@code mail.brevoApiKey} configurada, envia pela API HTTP da Brevo (plano gratuito: 300 e-mails/dia,
 * basta verificar o e-mail remetente; sem SMTP nem bibliotecas extras). Sem chave, o e-mail é apenas escrito no
 * log do servidor, o que serve para desenvolvimento local.</p>
 *
 * <p>O envio corre numa thread própria: o tempo de resposta da página é o mesmo exista ou não a conta
 * (não revela quais e-mails estão cadastrados).</p>
 */
public class EmailService {

    private static final Logger LOG = Logger.getLogger(EmailService.class.getName());
    private static final URI BREVO = URI.create("https://api.brevo.com/v3/smtp/email");

    private final String apiKey;
    private final String remetente;
    private final String nomeRemetente;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(8)).build();
    private final ExecutorService fila = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "ongsave-email");
        t.setDaemon(true);
        return t;
    });

    public EmailService(String apiKey, String remetente, String nomeRemetente) {
        this.apiKey = apiKey == null ? "" : apiKey.trim();
        this.remetente = remetente == null ? "" : remetente.trim();
        this.nomeRemetente = nomeRemetente == null || nomeRemetente.isBlank() ? "OngSave" : nomeRemetente.trim();
    }

    /** true quando há um provedor real configurado (senão os e-mails só vão para o log). */
    public boolean configurado() {
        return !apiKey.isEmpty() && !remetente.isEmpty();
    }

    /** Agenda o envio e volta logo. Falhas ficam no log, nunca chegam ao utilizador. */
    public void enviar(String para, String assunto, String texto, String html) {
        fila.submit(() -> {
            if (!configurado()) {
                LOG.warning(() -> "OngSave: e-mail NÃO enviado (mail.brevoApiKey/mail.remetente vazios). Para: " + para
                        + " | Assunto: " + assunto + "\n" + texto);
                return;
            }
            try {
                String corpo = Json.escrever(Json.obj(
                        "sender", Json.obj("email", remetente, "name", nomeRemetente),
                        "to", List.of(Json.obj("email", para)),
                        "subject", assunto,
                        "textContent", texto,
                        "htmlContent", html));
                HttpRequest req = HttpRequest.newBuilder(BREVO)
                        .timeout(Duration.ofSeconds(15))
                        .header("api-key", apiKey)
                        .header("Content-Type", "application/json")
                        .header("Accept", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(corpo))
                        .build();
                HttpResponse<String> r = http.send(req, HttpResponse.BodyHandlers.ofString());
                if (r.statusCode() / 100 != 2)
                    LOG.warning(() -> "OngSave: a Brevo recusou o e-mail (HTTP " + r.statusCode() + "): " + r.body());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (Exception e) {
                LOG.log(Level.WARNING, "OngSave: falha ao enviar e-mail", e);
            }
        });
    }

    public void parar() {
        fila.shutdownNow();
    }
}
