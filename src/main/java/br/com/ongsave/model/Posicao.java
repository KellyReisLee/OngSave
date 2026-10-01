package br.com.ongsave.model;

import java.time.Instant;

/** Posição GPS do motorista durante uma entrega (tabela posicoes). */
public class Posicao implements java.io.Serializable {
    private static final long serialVersionUID = 1L;

    private long id;
    private long loteId;
    private long motoristaId;
    private double lat;
    private double lon;
    private Float precisaoM;  // metros
    private Instant registadoEm;

    public Posicao() {}

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public long getLoteId() { return loteId; }
    public void setLoteId(long loteId) { this.loteId = loteId; }
    public long getMotoristaId() { return motoristaId; }
    public void setMotoristaId(long motoristaId) { this.motoristaId = motoristaId; }
    public double getLat() { return lat; }
    public void setLat(double lat) { this.lat = lat; }
    public double getLon() { return lon; }
    public void setLon(double lon) { this.lon = lon; }
    public Float getPrecisaoM() { return precisaoM; }
    public void setPrecisaoM(Float precisaoM) { this.precisaoM = precisaoM; }
    public Instant getRegistadoEm() { return registadoEm; }
    public void setRegistadoEm(Instant registadoEm) { this.registadoEm = registadoEm; }
}
