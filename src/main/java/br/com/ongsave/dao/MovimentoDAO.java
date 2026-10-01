package br.com.ongsave.dao;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import br.com.ongsave.db.Sql;
import br.com.ongsave.model.Movimento;

/** Acesso à tabela movimentos (carteira do motorista: fretes, saques e ajustes). */
public class MovimentoDAO {

    public void inserir(Connection c, Movimento m) throws SQLException {
        Sql.executar(c, "INSERT INTO movimentos (motorista_id, lote_id, tipo, descricao, valor, status) VALUES (?, ?, ?, ?, ?, ?)",
                m.getMotoristaId(), m.getLoteId(), m.getTipo(), m.getDescricao(), m.getValor(), m.getStatus());
    }

    /** Situação do frete de um lote (retido, disponivel, estornado...) ou null. */
    public String statusDoFrete(Connection c, long loteId) throws SQLException {
        return (String) Sql.valor(c, "SELECT status FROM movimentos WHERE lote_id = ? AND tipo = 'frete'", loteId);
    }

    /** Frete retido do lote, bloqueado para a decisão do admin; null se não houver. */
    public Movimento buscarFreteRetido(Connection c, long loteId) throws SQLException {
        Map<String, Object> m = Sql.linha(c, "SELECT id, motorista_id, lote_id, tipo, descricao, valor, status, criado_em FROM movimentos "
                + "WHERE lote_id = ? AND tipo = 'frete' AND status = 'retido' FOR UPDATE", loteId);
        if (m == null) return null;
        Movimento mv = new Movimento();
        mv.setId(Linhas.l(m, "id"));
        mv.setMotoristaId(Linhas.l(m, "motorista_id"));
        mv.setLoteId(Linhas.lng(m, "lote_id"));
        mv.setTipo(Linhas.s(m, "tipo"));
        mv.setDescricao(Linhas.s(m, "descricao"));
        mv.setValor(Linhas.d(m, "valor"));
        mv.setStatus(Linhas.s(m, "status"));
        mv.setCriadoEm(Linhas.instante(m, "criado_em"));
        return mv;
    }

    /** Extrato do motorista (mais recentes primeiro). */
    public List<Map<String, Object>> extrato(Connection c, long motoristaId) throws SQLException {
        return Sql.lista(c, """
                SELECT descricao, valor, status, FLOOR(EXTRACT(EPOCH FROM (now() - criado_em)) / 86400)::int AS dias
                FROM movimentos WHERE motorista_id = ? ORDER BY criado_em DESC LIMIT 200""", motoristaId);
    }

    /** Fretes não estornados dos últimos 28 dias, com a semana (0 = atual). */
    public List<Map<String, Object>> fretesUltimas4Semanas(Connection c, long motoristaId) throws SQLException {
        return Sql.lista(c, "SELECT valor, FLOOR(EXTRACT(EPOCH FROM (now() - criado_em)) / 604800)::int AS s "
                + "FROM movimentos WHERE motorista_id = ? AND tipo = 'frete' AND status <> 'estornado' AND criado_em > now() - interval '28 days'",
                motoristaId);
    }

    /** Saldo que o motorista pode sacar (já descontados os saques). */
    public double saldoDisponivel(Connection c, long motoristaId) throws SQLException {
        return Sql.decimal(c, "SELECT COALESCE(sum(valor), 0) FROM movimentos WHERE motorista_id = ? "
                + "AND status IN ('disponivel', 'solicitado', 'pago')", motoristaId);
    }

    /** Conferência sem problemas: o frete retido passa a disponível. */
    public void liberarFreteDoLote(Connection c, long loteId) throws SQLException {
        Sql.executar(c, "UPDATE movimentos SET status = 'disponivel' WHERE lote_id = ? AND tipo = 'frete' AND status = 'retido'", loteId);
    }

    public void ajustarValor(Connection c, long id, double valor) throws SQLException {
        Sql.executar(c, "UPDATE movimentos SET valor = ?, status = 'disponivel', descricao = left(descricao || ' · ajustado', 200) WHERE id = ?", valor, id);
    }

    public void atualizarStatus(Connection c, long id, String status) throws SQLException {
        Sql.executar(c, "UPDATE movimentos SET status = ? WHERE id = ?", status, id);
    }
}
