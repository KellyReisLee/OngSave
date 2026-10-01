package br.com.ongsave.dao;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import br.com.ongsave.db.Sql;
import br.com.ongsave.model.Ocorrencia;

/** Acesso à tabela ocorrencias (problemas reportados nas entregas). */
public class OcorrenciaDAO {

    public void inserir(Connection c, Ocorrencia o) throws SQLException {
        Sql.executar(c, "INSERT INTO ocorrencias (lote_id, tipo, obs, autor_id) VALUES (?, ?, ?, ?)",
                o.getLoteId(), o.getTipo(), o.getObs(), o.getAutorId());
    }

    public Ocorrencia buscar(Connection c, long id, boolean paraAtualizar) throws SQLException {
        Map<String, Object> m = Sql.linha(c, "SELECT id, lote_id, tipo, status, obs, resolucao, autor_id, criado_em, resolvido_em "
                + "FROM ocorrencias WHERE id = ?" + (paraAtualizar ? " FOR UPDATE" : ""), id);
        if (m == null) return null;
        Ocorrencia o = new Ocorrencia();
        o.setId(Linhas.l(m, "id"));
        o.setLoteId(Linhas.lng(m, "lote_id"));
        o.setTipo(Linhas.s(m, "tipo"));
        o.setStatus(Linhas.s(m, "status"));
        o.setObs(Linhas.s(m, "obs"));
        o.setResolucao(Linhas.s(m, "resolucao"));
        o.setAutorId(Linhas.lng(m, "autor_id"));
        o.setCriadoEm(Linhas.instante(m, "criado_em"));
        o.setResolvidoEm(Linhas.instante(m, "resolvido_em"));
        return o;
    }

    /** Passa de "aberta" para "em análise". Devolve 0 se já não estava aberta. */
    public int iniciarAnalise(Connection c, long id) throws SQLException {
        return Sql.executar(c, "UPDATE ocorrencias SET status = 'analise' WHERE id = ? AND status = 'aberta'", id);
    }

    public void resolver(Connection c, long id, String resolucao) throws SQLException {
        Sql.executar(c, "UPDATE ocorrencias SET status = 'resolvida', resolucao = ?, resolvido_em = now() WHERE id = ?", resolucao, id);
    }

    /** Há outra ocorrência do mesmo lote ainda pendente? */
    public boolean existemOutrasPendentes(Connection c, long loteId, long excetoId) throws SQLException {
        return Sql.numero(c, "SELECT count(*) FROM ocorrencias WHERE lote_id = ? AND id <> ? AND status <> 'resolvida'", loteId, excetoId) > 0;
    }

    /** Lista do painel do admin, com empresa, ONG, motorista e situação do frete. */
    public List<Map<String, Object>> listarParaAdministracao(Connection c) throws SQLException {
        return Sql.lista(c, """
                SELECT oc.id, oc.tipo, oc.lote_id AS lote, COALESCE(uo.nome, '—') AS ong, COALESCE(ue.nome, '—') AS empresa,
                       COALESCE(um.nome, '—') AS motorista, oc.status, (EXTRACT(EPOCH FROM oc.criado_em) * 1000)::bigint AS t,
                       COALESCE(oc.obs, '') AS obs, COALESCE(oc.resolucao, '') AS resolucao, l.frete_retido AS retido, l.frete
                FROM ocorrencias oc
                LEFT JOIN lotes l ON l.id = oc.lote_id
                LEFT JOIN usuarios uo ON uo.id = l.ong_id
                LEFT JOIN usuarios ue ON ue.id = l.empresa_id
                LEFT JOIN usuarios um ON um.id = l.motorista_id
                ORDER BY oc.criado_em DESC LIMIT 200""");
    }
}
