package br.com.ongsave.model;

import java.time.Instant;
import java.time.LocalDate;

/** Fatura mensal da assinatura de uma empresa (tabela faturas). */
public class Fatura implements java.io.Serializable {
    private static final long serialVersionUID = 1L;

    private long id;
    private long empresaId;
    private LocalDate referencia;  // dia 1 do mês
    private String plano;
    private double valor;
    private double extras;
    private String status;  // aberta | paga | atrasada
    private Instant criadoEm;

    public Fatura() {}

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public long getEmpresaId() { return empresaId; }
    public void setEmpresaId(long empresaId) { this.empresaId = empresaId; }
    public LocalDate getReferencia() { return referencia; }
    public void setReferencia(LocalDate referencia) { this.referencia = referencia; }
    public String getPlano() { return plano; }
    public void setPlano(String plano) { this.plano = plano; }
    public double getValor() { return valor; }
    public void setValor(double valor) { this.valor = valor; }
    public double getExtras() { return extras; }
    public void setExtras(double extras) { this.extras = extras; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Instant getCriadoEm() { return criadoEm; }
    public void setCriadoEm(Instant criadoEm) { this.criadoEm = criadoEm; }
}
