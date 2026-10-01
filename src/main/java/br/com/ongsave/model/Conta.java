package br.com.ongsave.model;

import java.time.Instant;

/** Linha da tabela usuarios (dados de acesso e situação da conta). */
public class Conta implements java.io.Serializable {
    private static final long serialVersionUID = 1L;

    private long id;
    private Perfil perfil;
    private String nome;
    private String email;
    private String senhaHash;  // PBKDF2
    private String status;  // pendente | ativo | suspenso | bloqueado | rejeitado
    private String documento;  // CNPJ ou CPF
    private String telefone;
    private Instant criadoEm;
    private Instant ultimoAcesso;
    private int falhasLogin;
    private Instant bloqueioAte;

    public Conta() {}

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public Perfil getPerfil() { return perfil; }
    public void setPerfil(Perfil perfil) { this.perfil = perfil; }
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getSenhaHash() { return senhaHash; }
    public void setSenhaHash(String senhaHash) { this.senhaHash = senhaHash; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getDocumento() { return documento; }
    public void setDocumento(String documento) { this.documento = documento; }
    public String getTelefone() { return telefone; }
    public void setTelefone(String telefone) { this.telefone = telefone; }
    public Instant getCriadoEm() { return criadoEm; }
    public void setCriadoEm(Instant criadoEm) { this.criadoEm = criadoEm; }
    public Instant getUltimoAcesso() { return ultimoAcesso; }
    public void setUltimoAcesso(Instant ultimoAcesso) { this.ultimoAcesso = ultimoAcesso; }
    public int getFalhasLogin() { return falhasLogin; }
    public void setFalhasLogin(int falhasLogin) { this.falhasLogin = falhasLogin; }
    public Instant getBloqueioAte() { return bloqueioAte; }
    public void setBloqueioAte(Instant bloqueioAte) { this.bloqueioAte = bloqueioAte; }

    public boolean isAtiva() { return "ativo".equals(status); }

    public boolean isBloqueadaTemporariamente() { return bloqueioAte != null && bloqueioAte.isAfter(Instant.now()); }

    /** Versão enxuta guardada na sessão. */
    public Usuario paraSessao() { return new Usuario(id, nome, email, perfil); }
}
