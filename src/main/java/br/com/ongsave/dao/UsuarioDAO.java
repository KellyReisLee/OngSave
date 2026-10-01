package br.com.ongsave.dao;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import br.com.ongsave.db.Sql;
import br.com.ongsave.model.Conta;
import br.com.ongsave.model.Perfil;

/** Acesso à tabela usuarios (contas de todos os perfis). */
public class UsuarioDAO {

    private static final String COLUNAS = "id, perfil, nome, email, senha_hash, status, documento, telefone, criado_em, "
            + "ultimo_acesso, falhas_login, bloqueio_ate";

    /* ---------- Consultas ---------- */

    public Conta buscarPorId(Connection c, long id, boolean paraAtualizar) throws SQLException {
        return conta(Sql.linha(c, "SELECT " + COLUNAS + " FROM usuarios WHERE id = ?" + (paraAtualizar ? " FOR UPDATE" : ""), id));
    }

    public Conta buscarPorEmail(Connection c, String email, boolean paraAtualizar) throws SQLException {
        return conta(Sql.linha(c, "SELECT " + COLUNAS + " FROM usuarios WHERE lower(email) = lower(?)" + (paraAtualizar ? " FOR UPDATE" : ""),
                email.trim()));
    }

    public List<Conta> listarPorPerfil(Connection c, Perfil perfil) throws SQLException {
        List<Conta> r = new ArrayList<>();
        for (Map<String, Object> m : Sql.lista(c, "SELECT " + COLUNAS + " FROM usuarios WHERE perfil = ? ORDER BY id", perfil.getSegmento()))
            r.add(conta(m));
        return r;
    }

    public boolean existe(Connection c, long id) throws SQLException {
        return Sql.numero(c, "SELECT count(*) FROM usuarios WHERE id = ?", id) > 0;
    }

    public boolean existeAtivo(Connection c, long id, Perfil perfil) throws SQLException {
        return Sql.numero(c, "SELECT count(*) FROM usuarios WHERE id = ? AND perfil = ? AND status = 'ativo'", id, perfil.getSegmento()) > 0;
    }

    public boolean emailExiste(Connection c, String email) throws SQLException {
        return Sql.numero(c, "SELECT count(*) FROM usuarios WHERE lower(email) = lower(?)", email.trim()) > 0;
    }

    public boolean emailEmUsoPorOutro(Connection c, String email, long id) throws SQLException {
        return Sql.numero(c, "SELECT count(*) FROM usuarios WHERE lower(email) = lower(?) AND id <> ?", email, id) > 0;
    }

    public boolean documentoExiste(Connection c, String documento, Perfil perfil) throws SQLException {
        return Sql.numero(c, "SELECT count(*) FROM usuarios WHERE documento = ? AND perfil = ?", documento, perfil.getSegmento()) > 0;
    }

    public String status(Connection c, long id) throws SQLException {
        return (String) Sql.valor(c, "SELECT status FROM usuarios WHERE id = ?", id);
    }

    public String nome(Connection c, long id) throws SQLException {
        return (String) Sql.valor(c, "SELECT nome FROM usuarios WHERE id = ?", id);
    }

    /** Lista do painel "Utilizadores" do admin, já com os dados do perfil de cada conta. */
    public List<Map<String, Object>> listarParaAdministracao(Connection c) throws SQLException {
        return Sql.lista(c, """
                SELECT u.id, u.perfil AS tipo, u.nome, COALESCE(u.documento, '') AS doc, u.status, u.cidade,
                       u.email, COALESCE(u.telefone, '') AS tel,
                       FLOOR(EXTRACT(EPOCH FROM (now() - u.criado_em)) / 86400)::int AS criado,
                       FLOOR(EXTRACT(EPOCH FROM (now() - u.ultimo_acesso)) / 86400)::int AS ultimo,
                       e.plano, COALESCE(e.responsavel, o.responsavel) AS resp,
                       m.veiculo, COALESCE(m.placa, '') AS placa, m.cnh_categoria AS "cnhCat", (m.cnh_validade - current_date) AS "cnhDias",
                       o.familias, o.capacidade_kg AS "capKg", (o.alvara_validade - current_date) AS "alvaraDias"
                FROM usuarios u
                LEFT JOIN empresas e ON e.usuario_id = u.id
                LEFT JOIN motoristas m ON m.usuario_id = u.id
                LEFT JOIN ongs o ON o.usuario_id = u.id
                WHERE u.perfil <> 'admin'
                ORDER BY u.criado_em DESC""");
    }

    /* ---------- Escrita ---------- */

    /** Cria a conta e devolve o id gerado. */
    public long inserir(Connection c, Conta u) throws SQLException {
        return Sql.inserir(c, "INSERT INTO usuarios (perfil, nome, email, senha_hash, status, documento, telefone) VALUES (?, ?, ?, ?, ?, ?, ?)",
                u.getPerfil().getSegmento(), u.getNome(), u.getEmail(), u.getSenhaHash(), u.getStatus(), u.getDocumento(), u.getTelefone());
    }

    public void registrarFalhaLogin(Connection c, long id, int falhas) throws SQLException {
        Sql.executar(c, "UPDATE usuarios SET falhas_login = ? WHERE id = ?", falhas, id);
    }

    public void bloquearTemporariamente(Connection c, long id, int minutos) throws SQLException {
        Sql.executar(c, "UPDATE usuarios SET falhas_login = 0, bloqueio_ate = now() + make_interval(mins => ?) WHERE id = ?", minutos, id);
    }

    public void registrarAcesso(Connection c, long id) throws SQLException {
        Sql.executar(c, "UPDATE usuarios SET falhas_login = 0, bloqueio_ate = NULL, ultimo_acesso = now() WHERE id = ?", id);
    }

    public void atualizarContato(Connection c, long id, String nome, String email, String telefone) throws SQLException {
        Sql.executar(c, "UPDATE usuarios SET nome = ?, email = ?, telefone = ? WHERE id = ?", nome, email.toLowerCase(), telefone, id);
    }

    public void atualizarSenha(Connection c, long id, String senhaHash) throws SQLException {
        Sql.executar(c, "UPDATE usuarios SET senha_hash = ? WHERE id = ?", senhaHash, id);
    }

    /** Zera o contador de senhas erradas e o bloqueio temporário (após recuperar a senha). */
    public void limparBloqueio(Connection c, long id) throws SQLException {
        Sql.executar(c, "UPDATE usuarios SET falhas_login = 0, bloqueio_ate = NULL WHERE id = ?", id);
    }

    /** Muda a situação da conta; ao (re)ativar, limpa o bloqueio por senhas erradas. */
    public void atualizarStatus(Connection c, long id, String status) throws SQLException {
        Sql.executar(c, "UPDATE usuarios SET status = ?" + ("ativo".equals(status) ? ", falhas_login = 0, bloqueio_ate = NULL" : "")
                + " WHERE id = ?", status, id);
    }

    /* ---------- Mapeamento ---------- */

    private static Conta conta(Map<String, Object> m) {
        if (m == null) return null;
        Conta u = new Conta();
        u.setId(Linhas.l(m, "id"));
        u.setPerfil(Perfil.doSegmento(Linhas.s(m, "perfil")).orElse(null));
        u.setNome(Linhas.s(m, "nome"));
        u.setEmail(Linhas.s(m, "email"));
        u.setSenhaHash(Linhas.s(m, "senha_hash"));
        u.setStatus(Linhas.s(m, "status"));
        u.setDocumento(Linhas.s(m, "documento"));
        u.setTelefone(Linhas.s(m, "telefone"));
        u.setCriadoEm(Linhas.instante(m, "criado_em"));
        u.setUltimoAcesso(Linhas.instante(m, "ultimo_acesso"));
        u.setFalhasLogin(Linhas.i(m, "falhas_login"));
        u.setBloqueioAte(Linhas.instante(m, "bloqueio_ate"));
        return u;
    }
}
