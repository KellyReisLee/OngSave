package br.com.ongsave.model;

import java.time.Instant;

/** Movimento da carteira do motorista (tabela movimentos). */
public class Movimento implements java.io.Serializable {
    private static final long serialVersionUID = 1L;

    private long id;
    private long motoristaId;
    private Long loteId;
    private String tipo;  // frete | saque | ajuste
    private String descricao;
    private double valor;  // negativo nos saques
    private String status;  // disponivel | retido | estornado | solicitado | pago
    private Instant criadoEm;

    public Movimento() {}

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public long getMotoristaId() { return motoristaId; }
    public void setMotoristaId(long motoristaId) { this.motoristaId = motoristaId; }
    public Long getLoteId() { return loteId; }
    public void setLoteId(Long loteId) { this.loteId = loteId; }
    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }
    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }
    public double getValor() { return valor; }
    public void setValor(double valor) { this.valor = valor; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Instant getCriadoEm() { return criadoEm; }
    public void setCriadoEm(Instant criadoEm) { this.criadoEm = criadoEm; }
}
