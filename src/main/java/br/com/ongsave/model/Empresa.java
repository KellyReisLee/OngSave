package br.com.ongsave.model;

/** Empresa doadora (tabela empresas, 1:1 com usuarios). */
public class Empresa implements java.io.Serializable {
    private static final long serialVersionUID = 1L;

    private long usuarioId;
    private String setor;
    private String responsavel;
    private String plano;  // pequena | media | grande
    private Double custoDescarte;  // R$/mês, opcional
    private String cep;
    private String rua;
    private String numero;
    private String complemento;
    private String bairro;
    private String cidade;
    private String uf;
    private Double lat;  // null enquanto o endereço não foi geocodificado
    private Double lon;

    public Empresa() {}

    public long getUsuarioId() { return usuarioId; }
    public void setUsuarioId(long usuarioId) { this.usuarioId = usuarioId; }
    public String getSetor() { return setor; }
    public void setSetor(String setor) { this.setor = setor; }
    public String getResponsavel() { return responsavel; }
    public void setResponsavel(String responsavel) { this.responsavel = responsavel; }
    public String getPlano() { return plano; }
    public void setPlano(String plano) { this.plano = plano; }
    public Double getCustoDescarte() { return custoDescarte; }
    public void setCustoDescarte(Double custoDescarte) { this.custoDescarte = custoDescarte; }
    public String getCep() { return cep; }
    public void setCep(String cep) { this.cep = cep; }
    public String getRua() { return rua; }
    public void setRua(String rua) { this.rua = rua; }
    public String getNumero() { return numero; }
    public void setNumero(String numero) { this.numero = numero; }
    public String getComplemento() { return complemento; }
    public void setComplemento(String complemento) { this.complemento = complemento; }
    public String getBairro() { return bairro; }
    public void setBairro(String bairro) { this.bairro = bairro; }
    public String getCidade() { return cidade; }
    public void setCidade(String cidade) { this.cidade = cidade; }
    public String getUf() { return uf; }
    public void setUf(String uf) { this.uf = uf; }
    public Double getLat() { return lat; }
    public void setLat(Double lat) { this.lat = lat; }
    public Double getLon() { return lon; }
    public void setLon(Double lon) { this.lon = lon; }
}
