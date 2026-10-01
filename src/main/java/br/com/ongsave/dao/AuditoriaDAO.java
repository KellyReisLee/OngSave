package br.com.ongsave.dao;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import br.com.ongsave.db.Sql;
import br.com.ongsave.model.RegistroAuditoria;

/** Acesso à tabela auditoria. */
public class AuditoriaDAO {

    public void inserir(Connection c, RegistroAuditoria r) throws SQLException {
        Sql.executar(c, "INSERT INTO auditoria (ator_id, ator_nome, acao, alvo, alvo_id, extra) VALUES (?, ?, ?, ?, ?, ?)",
                r.getAtorId(), r.getAtorNome(), r.getAcao(), r.getAlvo(), r.getAlvoId(), r.getExtra());
    }

    /** Últimos registos no formato do painel do admin. */
    public List<Map<String, Object>> listarRecentes(Connection c, int limite) throws SQLException {
        return Sql.lista(c, """
                SELECT (EXTRACT(EPOCH FROM criado_em) * 1000)::bigint AS t, ator_nome AS ator, acao, alvo, alvo_id AS "alvoId",
                       COALESCE(extra, '') AS extra
                FROM auditoria ORDER BY criado_em DESC LIMIT ?""", limite);
    }
}
