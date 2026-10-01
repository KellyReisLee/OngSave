package br.com.ongsave.model;

import java.time.Instant;
import java.time.LocalTime;

/** Lote de doação (tabela lotes): publicação, coleta, token, entrega e conferência. */
public class Lote implements java.io.Serializable {
    private static final long serialVersionUID = 1L;

    private long id;
    private long empresaId;
    private Long ongId;
    private Long motoristaId;
    private String categoria;
    private double pesoKg;
    private int volumes;
    private String conservacao;  // amb | ref | cong
    private String veiculo;  // carro | van | cam
    private Instant validade;
    private LocalTime janelaInicio;
    private LocalTime janelaFim;
    private String observacao;
    private byte[] foto;  // só preenchida na publicação
    private String fotoTipo;
    private String estado;  // aguardando | aceito | transito | entregue | cancelado
    private Double frete;
    private Instant criadoEm;
    private Instant ongAceiteEm;
    private Instant aceitoEm;
    private Instant canceladoEm;
    private String retiradaCodigo;
    private Double retiradaKg;
    private Double retiradaTemp;
    private String retiradaObs;
    private Instant retiradaEm;
    private Instant coletaEm;
    private String tokenCodigo;
    private Instant tokenGeradoEm;
    private Instant tokenExpiraEm;
    private Instant tokenUsadoEm;
    private int tokenTentativas;
    private String entregaFotoTipo;
    private Double entregaLat;
    private Double entregaLon;
    private Integer entregaDistM;
    private Instant fotoCapturadaEm;
    private Instant entregueEm;
    private Double confKg;
    private String confCondicao;  // ok | parcial | improprio
    private String confObs;
    private Double confDivPct;
    private Instant confEm;
    private boolean freteRetido;
    private boolean simulado;  // criado pelo modo simulação

    public Lote() {}

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public long getEmpresaId() { return empresaId; }
    public void setEmpresaId(long empresaId) { this.empresaId = empresaId; }
    public Long getOngId() { return ongId; }
    public void setOngId(Long ongId) { this.ongId = ongId; }
    public Long getMotoristaId() { return motoristaId; }
    public void setMotoristaId(Long motoristaId) { this.motoristaId = motoristaId; }
    public String getCategoria() { return categoria; }
    public void setCategoria(String categoria) { this.categoria = categoria; }
    public double getPesoKg() { return pesoKg; }
    public void setPesoKg(double pesoKg) { this.pesoKg = pesoKg; }
    public int getVolumes() { return volumes; }
    public void setVolumes(int volumes) { this.volumes = volumes; }
    public String getConservacao() { return conservacao; }
    public void setConservacao(String conservacao) { this.conservacao = conservacao; }
    public String getVeiculo() { return veiculo; }
    public void setVeiculo(String veiculo) { this.veiculo = veiculo; }
    public Instant getValidade() { return validade; }
    public void setValidade(Instant validade) { this.validade = validade; }
    public LocalTime getJanelaInicio() { return janelaInicio; }
    public void setJanelaInicio(LocalTime janelaInicio) { this.janelaInicio = janelaInicio; }
    public LocalTime getJanelaFim() { return janelaFim; }
    public void setJanelaFim(LocalTime janelaFim) { this.janelaFim = janelaFim; }
    public String getObservacao() { return observacao; }
    public void setObservacao(String observacao) { this.observacao = observacao; }
    public byte[] getFoto() { return foto; }
    public void setFoto(byte[] foto) { this.foto = foto; }
    public String getFotoTipo() { return fotoTipo; }
    public void setFotoTipo(String fotoTipo) { this.fotoTipo = fotoTipo; }
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
    public Double getFrete() { return frete; }
    public void setFrete(Double frete) { this.frete = frete; }
    public Instant getCriadoEm() { return criadoEm; }
    public void setCriadoEm(Instant criadoEm) { this.criadoEm = criadoEm; }
    public Instant getOngAceiteEm() { return ongAceiteEm; }
    public void setOngAceiteEm(Instant ongAceiteEm) { this.ongAceiteEm = ongAceiteEm; }
    public Instant getAceitoEm() { return aceitoEm; }
    public void setAceitoEm(Instant aceitoEm) { this.aceitoEm = aceitoEm; }
    public Instant getCanceladoEm() { return canceladoEm; }
    public void setCanceladoEm(Instant canceladoEm) { this.canceladoEm = canceladoEm; }
    public String getRetiradaCodigo() { return retiradaCodigo; }
    public void setRetiradaCodigo(String retiradaCodigo) { this.retiradaCodigo = retiradaCodigo; }
    public Double getRetiradaKg() { return retiradaKg; }
    public void setRetiradaKg(Double retiradaKg) { this.retiradaKg = retiradaKg; }
    public Double getRetiradaTemp() { return retiradaTemp; }
    public void setRetiradaTemp(Double retiradaTemp) { this.retiradaTemp = retiradaTemp; }
    public String getRetiradaObs() { return retiradaObs; }
    public void setRetiradaObs(String retiradaObs) { this.retiradaObs = retiradaObs; }
    public Instant getRetiradaEm() { return retiradaEm; }
    public void setRetiradaEm(Instant retiradaEm) { this.retiradaEm = retiradaEm; }
    public Instant getColetaEm() { return coletaEm; }
    public void setColetaEm(Instant coletaEm) { this.coletaEm = coletaEm; }
    public String getTokenCodigo() { return tokenCodigo; }
    public void setTokenCodigo(String tokenCodigo) { this.tokenCodigo = tokenCodigo; }
    public Instant getTokenGeradoEm() { return tokenGeradoEm; }
    public void setTokenGeradoEm(Instant tokenGeradoEm) { this.tokenGeradoEm = tokenGeradoEm; }
    public Instant getTokenExpiraEm() { return tokenExpiraEm; }
    public void setTokenExpiraEm(Instant tokenExpiraEm) { this.tokenExpiraEm = tokenExpiraEm; }
    public Instant getTokenUsadoEm() { return tokenUsadoEm; }
    public void setTokenUsadoEm(Instant tokenUsadoEm) { this.tokenUsadoEm = tokenUsadoEm; }
    public int getTokenTentativas() { return tokenTentativas; }
    public void setTokenTentativas(int tokenTentativas) { this.tokenTentativas = tokenTentativas; }
    public String getEntregaFotoTipo() { return entregaFotoTipo; }
    public void setEntregaFotoTipo(String entregaFotoTipo) { this.entregaFotoTipo = entregaFotoTipo; }
    public Double getEntregaLat() { return entregaLat; }
    public void setEntregaLat(Double entregaLat) { this.entregaLat = entregaLat; }
    public Double getEntregaLon() { return entregaLon; }
    public void setEntregaLon(Double entregaLon) { this.entregaLon = entregaLon; }
    public Integer getEntregaDistM() { return entregaDistM; }
    public void setEntregaDistM(Integer entregaDistM) { this.entregaDistM = entregaDistM; }
    public Instant getFotoCapturadaEm() { return fotoCapturadaEm; }
    public void setFotoCapturadaEm(Instant fotoCapturadaEm) { this.fotoCapturadaEm = fotoCapturadaEm; }
    public Instant getEntregueEm() { return entregueEm; }
    public void setEntregueEm(Instant entregueEm) { this.entregueEm = entregueEm; }
    public Double getConfKg() { return confKg; }
    public void setConfKg(Double confKg) { this.confKg = confKg; }
    public String getConfCondicao() { return confCondicao; }
    public void setConfCondicao(String confCondicao) { this.confCondicao = confCondicao; }
    public String getConfObs() { return confObs; }
    public void setConfObs(String confObs) { this.confObs = confObs; }
    public Double getConfDivPct() { return confDivPct; }
    public void setConfDivPct(Double confDivPct) { this.confDivPct = confDivPct; }
    public Instant getConfEm() { return confEm; }
    public void setConfEm(Instant confEm) { this.confEm = confEm; }
    public boolean isFreteRetido() { return freteRetido; }
    public void setFreteRetido(boolean freteRetido) { this.freteRetido = freteRetido; }
    public boolean isSimulado() { return simulado; }
    public void setSimulado(boolean simulado) { this.simulado = simulado; }

    public boolean isAtivo() { return "aceito".equals(estado) || "transito".equals(estado); }

    public boolean isAceitoPelaOng() { return ongAceiteEm != null; }

    public boolean temRetirada() { return retiradaCodigo != null; }

    public boolean isTokenValido() {
        return tokenCodigo != null && tokenUsadoEm == null && tokenExpiraEm != null && tokenExpiraEm.isAfter(Instant.now());
    }

    /** Peso de referência para a conferência: o pesado na retirada, ou o declarado na publicação. */
    public double pesoDeOrigem() { return retiradaKg != null ? retiradaKg : pesoKg; }

    public boolean participa(Usuario u) {
        return switch (u.getPerfil()) {
            case ADMIN -> true;
            case EMPRESA -> empresaId == u.getId();
            case ONG -> ongId != null && ongId == u.getId();
            case MOTORISTA -> motoristaId != null && motoristaId == u.getId();
        };
    }
}
