package br.com.ongsave.dao;

import java.sql.Connection;
import java.sql.SQLException;

import br.com.ongsave.db.Sql;
import br.com.ongsave.model.Posicao;

/** Acesso à tabela posicoes (rastreio GPS durante a entrega). */
public class PosicaoDAO {

    public void inserir(Connection c, Posicao p) throws SQLException {
        Sql.executar(c, "INSERT INTO posicoes (lote_id, motorista_id, lat, lon, precisao_m) VALUES (?, ?, ?, ?, ?)",
                p.getLoteId(), p.getMotoristaId(), p.getLat(), p.getLon(), p.getPrecisaoM());
    }

    /** Já há posição deste lote nos últimos {@code segundos}? (evita gravar o GPS mais vezes do que o necessário) */
    public boolean existeRecente(Connection c, long loteId, int segundos) throws SQLException {
        return Sql.numero(c, "SELECT count(*) FROM posicoes WHERE lote_id = ? AND registado_em > now() - (? * interval '1 second')",
                loteId, segundos) > 0;
    }

    /** Retenção LGPD: apaga as posições mais antigas do que {@code dias}. */
    public int apagarAntigas(Connection c, int dias) throws SQLException {
        return Sql.executar(c, "DELETE FROM posicoes WHERE registado_em < now() - (? * interval '1 day')", dias);
    }
}
