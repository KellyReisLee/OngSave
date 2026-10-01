package br.com.ongsave.dao;

import java.sql.Connection;
import java.sql.SQLException;

import br.com.ongsave.db.Sql;

/** Acesso à tabela plataforma_config (linha única com os parâmetros em JSON). */
public class PlataformaDAO {

    /** Parâmetros em texto JSON, ou null se a linha ainda não existir. */
    public String lerDados(Connection c) throws SQLException {
        return (String) Sql.valor(c, "SELECT dados::text FROM plataforma_config WHERE id = 1");
    }

    public void gravarDados(Connection c, String json) throws SQLException {
        Sql.executar(c, "UPDATE plataforma_config SET dados = ?::jsonb, atualizado_em = now() WHERE id = 1", json);
    }
}
