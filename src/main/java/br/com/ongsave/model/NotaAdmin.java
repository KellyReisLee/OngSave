package br.com.ongsave.model;

import java.time.Instant;

/** Nota interna do administrador sobre uma conta (tabela notas_admin). */
public class NotaAdmin implements java.io.Serializable {
    private static final long serialVersionUID = 1L;

    private long id;
    private long usuarioId;
    private Long autorId;
    private String texto;
    private Instant criadoEm;

    public NotaAdmin() {}

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public long getUsuarioId() { return usuarioId; }
    public void setUsuarioId(long usuarioId) { this.usuarioId = usuarioId; }
    public Long getAutorId() { return autorId; }
    public void setAutorId(Long autorId) { this.autorId = autorId; }
    public String getTexto() { return texto; }
    public void setTexto(String texto) { this.texto = texto; }
    public Instant getCriadoEm() { return criadoEm; }
    public void setCriadoEm(Instant criadoEm) { this.criadoEm = criadoEm; }
}
