package br.com.ongsave.dao;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import br.com.ongsave.db.Sql;

/** Acesso à tabela faturas (assinatura mensal das empresas). */
public class FaturaDAO {

    /** Cria a fatura do mês corrente se ainda não existir (idempotente). */
    public void criarDoMesSeNaoExiste(Connection c, long empresaId, String plano, double valor) throws SQLException {
        Sql.executar(c, "INSERT INTO faturas (empresa_id, referencia, plano, valor) VALUES (?, date_trunc('month', now())::date, ?, ?) "
                + "ON CONFLICT (empresa_id, referencia) DO NOTHING", empresaId, plano, valor);
    }

    /** A fatura aberta do mês acompanha a troca de plano. */
    public void atualizarPlanoDoMes(Connection c, long empresaId, String plano, double valor) throws SQLException {
        Sql.executar(c, "UPDATE faturas SET plano = ?, valor = ? WHERE empresa_id = ? AND status = 'aberta' "
                + "AND referencia = date_trunc('month', now())::date", plano, valor, empresaId);
    }

    /** Últimas 12 faturas da empresa. */
    public List<Map<String, Object>> listarDaEmpresa(Connection c, long empresaId) throws SQLException {
        return Sql.lista(c, "SELECT id, to_char(referencia, 'YYYY-MM') AS ref, plano, valor, extras, status "
                + "FROM faturas WHERE empresa_id = ? ORDER BY referencia DESC LIMIT 12", empresaId);
    }
}
