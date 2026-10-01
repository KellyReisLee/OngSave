package br.com.ongsave.model;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/** Instituição recetora (tabela ongs, 1:1 com usuarios). */
public class Ong implements java.io.Serializable {
    private static final long serialVersionUID = 1L;

    private long usuarioId;
    private String razaoSocial;
    private String responsavel;
    private int familias;
    private double capacidadeKg;  // capacidade diária
    private LocalTime horaInicio;
    private LocalTime horaFim;
    private boolean camaraFria;
    private List<String> categorias;  // categorias aceites
    private LocalDate alvaraValidade;
    private String cep;
    private String rua;
    private String numero;
    private String complemento;
    private String bairro;
    private String cidade;
    private String uf;
    private Double lat;  // null enquanto o endereço não foi geocodificado
    private Double lon;

    public Ong() {}

    public long getUsuarioId() { return usuarioId; }
    public void setUsuarioId(long usuarioId) { this.usuarioId = usuarioId; }
    public String getRazaoSocial() { return razaoSocial; }
    public void setRazaoSocial(String razaoSocial) { this.razaoSocial = razaoSocial; }
    public String getResponsavel() { return responsavel; }
    public void setResponsavel(String responsavel) { this.responsavel = responsavel; }
    public int getFamilias() { return familias; }
    public void setFamilias(int familias) { this.familias = familias; }
    public double getCapacidadeKg() { return capacidadeKg; }
    public void setCapacidadeKg(double capacidadeKg) { this.capacidadeKg = capacidadeKg; }
    public LocalTime getHoraInicio() { return horaInicio; }
    public void setHoraInicio(LocalTime horaInicio) { this.horaInicio = horaInicio; }
    public LocalTime getHoraFim() { return horaFim; }
    public void setHoraFim(LocalTime horaFim) { this.horaFim = horaFim; }
    public boolean isCamaraFria() { return camaraFria; }
    public void setCamaraFria(boolean camaraFria) { this.camaraFria = camaraFria; }
    public List<String> getCategorias() { return categorias; }
    public void setCategorias(List<String> categorias) { this.categorias = categorias; }
    public LocalDate getAlvaraValidade() { return alvaraValidade; }
    public void setAlvaraValidade(LocalDate alvaraValidade) { this.alvaraValidade = alvaraValidade; }
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

    public boolean aceita(String categoria, String conservacao) {
        return categorias != null && categorias.contains(categoria) && ("amb".equals(conservacao) || camaraFria);
    }
}
