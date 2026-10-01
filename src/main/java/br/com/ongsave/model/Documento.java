package br.com.ongsave.model;

import java.time.Instant;

/** Documento de cadastro enviado para aprovação (tabela documentos). */
public class Documento implements java.io.Serializable {
    private static final long serialVersionUID = 1L;

    private long id;
    private long usuarioId;
    private String nome;
    private String arquivoNome;
    private byte[] conteudo;
    private String contentType;
    private String status;  // ok | analise | rejeitado
    private String motivo;
    private Instant atualizadoEm;

    public Documento() {}

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public long getUsuarioId() { return usuarioId; }
    public void setUsuarioId(long usuarioId) { this.usuarioId = usuarioId; }
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public String getArquivoNome() { return arquivoNome; }
    public void setArquivoNome(String arquivoNome) { this.arquivoNome = arquivoNome; }
    public byte[] getConteudo() { return conteudo; }
    public void setConteudo(byte[] conteudo) { this.conteudo = conteudo; }
    public String getContentType() { return contentType; }
    public void setContentType(String contentType) { this.contentType = contentType; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getMotivo() { return motivo; }
    public void setMotivo(String motivo) { this.motivo = motivo; }
    public Instant getAtualizadoEm() { return atualizadoEm; }
    public void setAtualizadoEm(Instant atualizadoEm) { this.atualizadoEm = atualizadoEm; }
}
