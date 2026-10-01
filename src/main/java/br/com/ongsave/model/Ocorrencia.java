package br.com.ongsave.model;

import java.time.Instant;

/** Ocorrência aberta sobre um lote (tabela ocorrencias). */
public class Ocorrencia implements java.io.Serializable {
    private static final long serialVersionUID = 1L;

    private long id;
    private Long loteId;
    private String tipo;
    private String status;  // aberta | analise | resolvida
    private String obs;
    private String resolucao;
    private Long autorId;
    private Instant criadoEm;
    private Instant resolvidoEm;

    public Ocorrencia() {}

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public Long getLoteId() { return loteId; }
    public void setLoteId(Long loteId) { this.loteId = loteId; }
    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getObs() { return obs; }
    public void setObs(String obs) { this.obs = obs; }
    public String getResolucao() { return resolucao; }
    public void setResolucao(String resolucao) { this.resolucao = resolucao; }
    public Long getAutorId() { return autorId; }
    public void setAutorId(Long autorId) { this.autorId = autorId; }
    public Instant getCriadoEm() { return criadoEm; }
    public void setCriadoEm(Instant criadoEm) { this.criadoEm = criadoEm; }
    public Instant getResolvidoEm() { return resolvidoEm; }
    public void setResolvidoEm(Instant resolvidoEm) { this.resolvidoEm = resolvidoEm; }

    public boolean isResolvida() { return "resolvida".equals(status); }
}
