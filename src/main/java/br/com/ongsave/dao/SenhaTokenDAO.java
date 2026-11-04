package br.com.ongsave.dao;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;

import br.com.ongsave.db.Sql;

/** Tokens de recuperação de senha (tabela senha_tokens). Só o hash SHA-256 do token é guardado. */
public class SenhaTokenDAO {

    public void inserir(Connection c, long usuarioId, String tokenHash, int minutos, String ip) throws SQLException {
        Sql.executar(c, "INSERT INTO senha_tokens (usuario_id, token_hash, expira_em, ip_pedido) "
                + "VALUES (?, ?, now() + (? * interval '1 minute'), ?)", usuarioId, tokenHash, minutos, ip);
    }

    /** Pedidos feitos para a conta na última hora (limita o envio de e-mails). */
    public long pedidosRecentes(Connection c, long usuarioId) throws SQLException {
        return Sql.numero(c, "SELECT count(*) FROM senha_tokens WHERE usuario_id = ? AND criado_em > now() - interval '1 hour'", usuarioId);
    }

    /** Token ainda válido (não usado e não expirado), bloqueado para atualização: {@code usuarioId} ou null. */
    public Long usuarioDoTokenValido(Connection c, String tokenHash) throws SQLException {
        Map<String, Object> m = Sql.linha(c, "SELECT usuario_id AS \"u\" FROM senha_tokens "
                + "WHERE token_hash = ? AND usado_em IS NULL AND expira_em > now() FOR UPDATE", tokenHash);
        return m == null ? null : ((Number) m.get("u")).longValue();
    }

    /** Marca como usados todos os tokens em aberto da conta (o usado e os anteriores). */
    public void consumirTodos(Connection c, long usuarioId) throws SQLException {
        Sql.executar(c, "UPDATE senha_tokens SET usado_em = now() WHERE usuario_id = ? AND usado_em IS NULL", usuarioId);
    }

    /** Limpeza periódica: tokens com mais de 7 dias. */
    public int apagarAntigos(Connection c) throws SQLException {
        return Sql.executar(c, "DELETE FROM senha_tokens WHERE criado_em < now() - interval '7 days'");
    }
}
