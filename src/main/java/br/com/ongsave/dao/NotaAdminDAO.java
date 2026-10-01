package br.com.ongsave.dao;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import br.com.ongsave.db.Sql;
import br.com.ongsave.model.NotaAdmin;

/** Acesso à tabela notas_admin (anotações internas sobre cada conta). */
public class NotaAdminDAO {

    public void inserir(Connection c, NotaAdmin n) throws SQLException {
        Sql.executar(c, "INSERT INTO notas_admin (usuario_id, autor_id, texto) VALUES (?, ?, ?)", n.getUsuarioId(), n.getAutorId(), n.getTexto());
    }

    /** Notas mais recentes de todas as contas (com usuario_id, t, txt e autor). */
    public List<Map<String, Object>> listarRecentes(Connection c, int limite) throws SQLException {
        return Sql.lista(c, """
                SELECT n.usuario_id, (EXTRACT(EPOCH FROM n.criado_em) * 1000)::bigint AS t, n.texto AS txt, COALESCE(a.nome, 'Administrador') AS autor
                FROM notas_admin n LEFT JOIN usuarios a ON a.id = n.autor_id ORDER BY n.criado_em DESC LIMIT ?""", limite);
    }
}
