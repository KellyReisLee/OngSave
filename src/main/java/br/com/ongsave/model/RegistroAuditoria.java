package br.com.ongsave.model;

import java.time.Instant;

/** Linha da trilha de auditoria (tabela auditoria). */
public class RegistroAuditoria implements java.io.Serializable {
    private static final long serialVersionUID = 1L;

    private long id;
    private Long atorId;
    private String atorNome;
    private String acao;
    private String alvo;
    private Long alvoId;
    private String extra;
    private Instant criadoEm;

    public RegistroAuditoria() {}

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public Long getAtorId() { return atorId; }
    public void setAtorId(Long atorId) { this.atorId = atorId; }
    public String getAtorNome() { return atorNome; }
    public void setAtorNome(String atorNome) { this.atorNome = atorNome; }
    public String getAcao() { return acao; }
    public void setAcao(String acao) { this.acao = acao; }
    public String getAlvo() { return alvo; }
    public void setAlvo(String alvo) { this.alvo = alvo; }
    public Long getAlvoId() { return alvoId; }
    public void setAlvoId(Long alvoId) { this.alvoId = alvoId; }
    public String getExtra() { return extra; }
    public void setExtra(String extra) { this.extra = extra; }
    public Instant getCriadoEm() { return criadoEm; }
    public void setCriadoEm(Instant criadoEm) { this.criadoEm = criadoEm; }
}
