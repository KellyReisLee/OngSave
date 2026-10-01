package br.com.ongsave.model;

/**
 * Regra de negócio violada. A mensagem é segura para mostrar ao utilizador
 * e o status HTTP diz ao front-end o que aconteceu (400, 403, 404, 409, 422...).
 */
public class ErroNegocio extends RuntimeException {
    private static final long serialVersionUID = 1L;
    private final int status;

    public ErroNegocio(int status, String mensagem) {
        super(mensagem, null, false, false); // sem stack trace: é um fluxo esperado
        this.status = status;
    }

    public int getStatus() { return status; }

    public static ErroNegocio invalido(String msg) { return new ErroNegocio(422, msg); }
    public static ErroNegocio naoEncontrado(String msg) { return new ErroNegocio(404, msg); }
    public static ErroNegocio conflito(String msg) { return new ErroNegocio(409, msg); }
    public static ErroNegocio proibido(String msg) { return new ErroNegocio(403, msg); }
}
