package br.com.ongsave.dao;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import br.com.ongsave.db.Sql;

/**
 * Consultas agregadas (só leitura) que cruzam várias tabelas para os painéis e relatórios da administração:
 * indicadores por conta, padrões de risco, atividade recente e finanças.
 */
public class RelatorioDAO {

    /** Entregas, kg e fretes retidos por utilizador (empresa, motorista ou ONG). Chave: id do utilizador. */
    public Map<Long, Map<String, Object>> estatisticasPorUsuario(Connection c) throws SQLException {
        Map<Long, Map<String, Object>> r = new HashMap<>();
        for (Map<String, Object> s : Sql.lista(c, """
                SELECT uid, count(*) AS entregas, round(sum(kg)) AS kg, count(*) FILTER (WHERE retido) AS retidos
                FROM (SELECT empresa_id AS uid, COALESCE(conf_kg, peso_kg) AS kg, frete_retido AS retido FROM lotes WHERE estado = 'entregue'
                      UNION ALL SELECT motorista_id, COALESCE(conf_kg, peso_kg), frete_retido FROM lotes WHERE estado = 'entregue'
                      UNION ALL SELECT ong_id, COALESCE(conf_kg, peso_kg), frete_retido FROM lotes WHERE estado = 'entregue') x
                WHERE uid IS NOT NULL GROUP BY uid"""))
            r.put(Linhas.l(s, "uid"), s);
        return r;
    }

    /** Ocorrências dos últimos 30 dias em que cada utilizador participou. */
    public Map<Long, Long> ocorrenciasPorUsuario30Dias(Connection c) throws SQLException {
        return contagem(c, """
                SELECT uid, count(*) AS n FROM (
                    SELECT l.empresa_id AS uid FROM ocorrencias oc JOIN lotes l ON l.id = oc.lote_id WHERE oc.criado_em > now() - interval '30 days'
                    UNION ALL SELECT l.motorista_id FROM ocorrencias oc JOIN lotes l ON l.id = oc.lote_id WHERE oc.criado_em > now() - interval '30 days'
                    UNION ALL SELECT l.ong_id FROM ocorrencias oc JOIN lotes l ON l.id = oc.lote_id WHERE oc.criado_em > now() - interval '30 days') x
                WHERE uid IS NOT NULL GROUP BY uid""");
    }

    public Map<Long, Long> recusasPorOng90Dias(Connection c) throws SQLException {
        return contagem(c, "SELECT ong_id AS uid, count(*) AS n FROM lote_recusas WHERE criado_em > now() - interval '90 days' GROUP BY ong_id");
    }

    public Map<Long, Long> aceitesPorOng90Dias(Connection c) throws SQLException {
        return contagem(c, "SELECT ong_id AS uid, count(*) AS n FROM lotes WHERE ong_aceite_em > now() - interval '90 days' GROUP BY ong_id");
    }

    public Map<Long, Long> lotesDoMesPorEmpresa(Connection c) throws SQLException {
        return contagem(c, "SELECT empresa_id AS uid, count(*) AS n FROM lotes WHERE estado <> 'cancelado' "
                + "AND criado_em >= date_trunc('month', now()) GROUP BY empresa_id");
    }

    /** Concentração motorista × ONG nos últimos 90 dias (sinal clássico de conluio para liberar pagamento). */
    public List<Map<String, Object>> paresMotoristaOng(Connection c) throws SQLException {
        return Sql.lista(c, """
                WITH t AS (
                    SELECT motorista_id, ong_id, count(*) AS n, sum(count(*)) OVER (PARTITION BY motorista_id) AS total
                    FROM lotes WHERE estado = 'entregue' AND entregue_em > now() - interval '90 days' AND motorista_id IS NOT NULL
                    GROUP BY motorista_id, ong_id)
                SELECT um.nome AS mot, uo.nome AS ong, round(100.0 * t.n / t.total)::int AS pct, t.total::int AS entregas
                FROM t JOIN usuarios um ON um.id = t.motorista_id JOIN usuarios uo ON uo.id = t.ong_id
                WHERE t.total >= 5
                ORDER BY 100.0 * t.n / t.total DESC, t.total DESC LIMIT 6""");
    }

    /** Eventos recentes da plataforma (entregas, cadastros, ocorrências, publicações). */
    public List<Map<String, Object>> atividadeRecente(Connection c) throws SQLException {
        return Sql.lista(c, """
                SELECT * FROM (
                    SELECT (EXTRACT(EPOCH FROM l.entregue_em) * 1000)::bigint AS t, 'fa-circle-check' AS ic,
                           'Lote #' || l.id || ' validado na ' || uo.nome || '.' AS txt
                    FROM lotes l JOIN usuarios uo ON uo.id = l.ong_id WHERE l.entregue_em > now() - interval '7 days'
                    UNION ALL
                    SELECT (EXTRACT(EPOCH FROM criado_em) * 1000)::bigint, 'fa-user-plus', 'Novo cadastro: ' || nome || ' (' || perfil || ').'
                    FROM usuarios WHERE perfil <> 'admin' AND criado_em > now() - interval '7 days'
                    UNION ALL
                    SELECT (EXTRACT(EPOCH FROM criado_em) * 1000)::bigint, 'fa-triangle-exclamation',
                           'Ocorrência aberta: ' || lower(tipo) || COALESCE(' no lote #' || lote_id, '') || '.'
                    FROM ocorrencias WHERE criado_em > now() - interval '7 days'
                    UNION ALL
                    SELECT (EXTRACT(EPOCH FROM criado_em) * 1000)::bigint, 'fa-bullhorn', 'Lote #' || id || ' publicado.'
                    FROM lotes WHERE criado_em > now() - interval '2 days'
                ) f ORDER BY t DESC LIMIT 12""");
    }

    /** Entregas por semana nas últimas 6 semanas (w = 0 é a semana atual). */
    public List<Map<String, Object>> entregasPorSemana(Connection c) throws SQLException {
        return Sql.lista(c, "SELECT FLOOR(EXTRACT(EPOCH FROM (now() - entregue_em)) / 604800)::int AS w, count(*) AS n "
                + "FROM lotes WHERE estado = 'entregue' AND entregue_em > now() - interval '42 days' GROUP BY 1");
    }

    /** Receita faturada por mês (YYYY-MM) nos últimos 6 meses. */
    public List<Map<String, Object>> receitaPorMes(Connection c) throws SQLException {
        return Sql.lista(c, "SELECT to_char(referencia, 'YYYY-MM') AS m, sum(valor + extras) AS v FROM faturas "
                + "WHERE referencia >= (date_trunc('month', now()) - interval '5 months')::date GROUP BY 1");
    }

    /** Fretes pagos/retidos por mês (YYYY-MM, fuso de São Paulo) nos últimos 6 meses. */
    public List<Map<String, Object>> fretesPorMes(Connection c) throws SQLException {
        return Sql.lista(c, "SELECT to_char(criado_em AT TIME ZONE 'America/Sao_Paulo', 'YYYY-MM') AS m, sum(valor) AS v "
                + "FROM movimentos WHERE tipo = 'frete' AND status <> 'estornado' AND criado_em >= date_trunc('month', now()) - interval '5 months' GROUP BY 1");
    }

    /** Últimos fretes e saques de todos os motoristas. */
    public List<Map<String, Object>> movimentosRecentes(Connection c) throws SQLException {
        return Sql.lista(c, """
                SELECT (EXTRACT(EPOCH FROM mv.criado_em) * 1000)::bigint AS t, mv.lote_id AS lote, u.nome AS mot, mv.valor, mv.status, mv.tipo
                FROM movimentos mv JOIN usuarios u ON u.id = mv.motorista_id
                WHERE mv.tipo IN ('frete', 'saque') ORDER BY mv.criado_em DESC LIMIT 60""");
    }

    /** Faturas dos últimos 3 meses de todas as empresas. */
    public List<Map<String, Object>> faturasRecentes(Connection c) throws SQLException {
        return Sql.lista(c, """
                SELECT (EXTRACT(EPOCH FROM f.referencia::timestamp) * 1000)::bigint AS t, u.nome AS emp, f.plano, f.valor, f.extras, f.status
                FROM faturas f JOIN usuarios u ON u.id = f.empresa_id
                WHERE f.referencia >= (date_trunc('month', now()) - interval '2 months')::date ORDER BY f.referencia DESC, u.nome LIMIT 100""");
    }

    private static Map<Long, Long> contagem(Connection c, String sql) throws SQLException {
        Map<Long, Long> m = new HashMap<>();
        for (Map<String, Object> l : Sql.lista(c, sql)) m.put(Linhas.l(l, "uid"), Linhas.l(l, "n"));
        return m;
    }
}
