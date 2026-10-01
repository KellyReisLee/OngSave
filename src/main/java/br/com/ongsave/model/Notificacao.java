package br.com.ongsave.model;

import java.time.Instant;

/** Notificação interna (sino do topo dos painéis). */
public class Notificacao implements java.io.Serializable {
    private static final long serialVersionUID = 1L;

    private long id;
    private long usuarioId;
    private String texto;
    private String icone;
    private boolean lida;
    private Instant criadoEm;

    public Notificacao() {}

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public long getUsuarioId() { return usuarioId; }
    public void setUsuarioId(long usuarioId) { this.usuarioId = usuarioId; }
    public String getTexto() { return texto; }
    public void setTexto(String texto) { this.texto = texto; }
    public String getIcone() { return icone; }
    public void setIcone(String icone) { this.icone = icone; }
    public boolean isLida() { return lida; }
    public void setLida(boolean lida) { this.lida = lida; }
    public Instant getCriadoEm() { return criadoEm; }
    public void setCriadoEm(Instant criadoEm) { this.criadoEm = criadoEm; }
}
