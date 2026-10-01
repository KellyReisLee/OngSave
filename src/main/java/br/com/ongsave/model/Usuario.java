package br.com.ongsave.model;

import java.io.Serializable;

/**
 * Utilizador guardado na sessão ("usuarioLogado").
 * Não guarda a senha: só o que as JSPs e as APIs precisam.
 */
public class Usuario implements Serializable {
    private static final long serialVersionUID = 2L;

    private final long id;
    private final String nome;
    private final String email;
    private final Perfil perfil;

    public Usuario(long id, String nome, String email, Perfil perfil) {
        this.id = id;
        this.nome = nome;
        this.email = email;
        this.perfil = perfil;
    }

    public long getId() { return id; }
    public String getNome() { return nome; }
    public String getEmail() { return email; }
    public Perfil getPerfil() { return perfil; }

    /** Cópia com o nome atualizado (ex.: depois de a empresa mudar a razão social). */
    public Usuario comNome(String novoNome) { return new Usuario(id, novoNome, email, perfil); }
}
