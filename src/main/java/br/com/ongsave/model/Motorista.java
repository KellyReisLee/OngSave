package br.com.ongsave.model;

import java.time.Instant;
import java.time.LocalDate;

/** Motorista parceiro (tabela motoristas, 1:1 com usuarios). */
public class Motorista implements java.io.Serializable {
    private static final long serialVersionUID = 1L;

    private long usuarioId;
    private String veiculo;  // Carro Económico | Camionete | Van | Caminhão
    private String placa;
    private String modelo;
    private String cnh;
    private String cnhCategoria;
    private LocalDate cnhValidade;
    private String cep;
    private String rua;
    private String numero;
    private String bairro;
    private String cidade;
    private String uf;
    private Instant consentimentoEm;  // consentimento LGPD de localização; null = sem consentimento

    public Motorista() {}

    public long getUsuarioId() { return usuarioId; }
    public void setUsuarioId(long usuarioId) { this.usuarioId = usuarioId; }
    public String getVeiculo() { return veiculo; }
    public void setVeiculo(String veiculo) { this.veiculo = veiculo; }
    public String getPlaca() { return placa; }
    public void setPlaca(String placa) { this.placa = placa; }
    public String getModelo() { return modelo; }
    public void setModelo(String modelo) { this.modelo = modelo; }
    public String getCnh() { return cnh; }
    public void setCnh(String cnh) { this.cnh = cnh; }
    public String getCnhCategoria() { return cnhCategoria; }
    public void setCnhCategoria(String cnhCategoria) { this.cnhCategoria = cnhCategoria; }
    public LocalDate getCnhValidade() { return cnhValidade; }
    public void setCnhValidade(LocalDate cnhValidade) { this.cnhValidade = cnhValidade; }
    public String getCep() { return cep; }
    public void setCep(String cep) { this.cep = cep; }
    public String getRua() { return rua; }
    public void setRua(String rua) { this.rua = rua; }
    public String getNumero() { return numero; }
    public void setNumero(String numero) { this.numero = numero; }
    public String getBairro() { return bairro; }
    public void setBairro(String bairro) { this.bairro = bairro; }
    public String getCidade() { return cidade; }
    public void setCidade(String cidade) { this.cidade = cidade; }
    public String getUf() { return uf; }
    public void setUf(String uf) { this.uf = uf; }
    public Instant getConsentimentoEm() { return consentimentoEm; }
    public void setConsentimentoEm(Instant consentimentoEm) { this.consentimentoEm = consentimentoEm; }

    public boolean isCnhVencida() { return cnhValidade != null && cnhValidade.isBefore(LocalDate.now()); }

    public boolean temConsentimento() { return consentimentoEm != null; }
}
