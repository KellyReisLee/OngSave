-- =====================================================================================
-- OngSave · Consultas de análise (PostgreSQL)
-- Rode depois de database/ongsave_neon.sql. Todas são só de leitura (SELECT).
-- Cada consulta responde a uma pergunta do negócio e usa JOIN e/ou agregação.
-- =====================================================================================


-- 1) Ranking das empresas doadoras: quantos lotes entregaram, quantos kg e quantas
--    refeições equivalentes (fórmula da plataforma: 1 kg = 2 refeições).
--    JOIN usuarios + empresas + lotes · COUNT, SUM, GROUP BY, ORDER BY
SELECT u.nome                                AS empresa,
       e.plano,
       COUNT(l.id)                           AS lotes_entregues,
       ROUND(SUM(l.peso_kg), 1)              AS kg_doados,
       ROUND(SUM(l.peso_kg) * 2)             AS refeicoes_equivalentes
FROM usuarios u
JOIN empresas e ON e.usuario_id = u.id
JOIN lotes    l ON l.empresa_id = u.id AND l.estado = 'entregue'
GROUP BY u.nome, e.plano
ORDER BY kg_doados DESC;


-- 2) Desempenho dos motoristas: entregas concluídas, kg transportados, frete ganho e
--    ticket médio por entrega, só para quem já entregou.
--    JOIN usuarios + motoristas + lotes · COUNT, SUM, AVG, HAVING
SELECT u.nome                                AS motorista,
       m.veiculo,
       COUNT(l.id)                           AS entregas,
       ROUND(SUM(l.peso_kg), 1)              AS kg_transportados,
       ROUND(SUM(l.frete), 2)                AS frete_total_rs,
       ROUND(AVG(l.frete), 2)                AS frete_medio_rs
FROM usuarios u
JOIN motoristas m ON m.usuario_id = u.id
JOIN lotes      l ON l.motorista_id = u.id AND l.estado = 'entregue'
GROUP BY u.nome, m.veiculo
HAVING COUNT(l.id) >= 1
ORDER BY entregas DESC, frete_total_rs DESC;


-- 3) Ocupação das ONGs hoje: capacidade diária, kg já comprometidos (a caminho,
--    aceites ou entregues hoje) e percentagem ocupada. É a mesma regra que a
--    plataforma usa para decidir se uma ONG pode receber mais um lote.
--    JOIN usuarios + ongs + LEFT JOIN lotes · SUM com FILTER, COALESCE
SELECT u.nome                                AS ong,
       o.capacidade_kg,
       COALESCE(SUM(l.peso_kg), 0)           AS kg_comprometidos_hoje,
       o.capacidade_kg - COALESCE(SUM(l.peso_kg), 0) AS kg_livres,
       ROUND(100.0 * COALESCE(SUM(l.peso_kg), 0) / NULLIF(o.capacidade_kg, 0), 1) AS ocupacao_pct
FROM usuarios u
JOIN ongs o       ON o.usuario_id = u.id
LEFT JOIN lotes l ON l.ong_id = u.id AND (
       l.estado IN ('aceito', 'transito')
    OR (l.estado = 'aguardando' AND l.ong_aceite_em IS NOT NULL)
    OR (l.estado = 'entregue'   AND l.entregue_em >= date_trunc('day', now())))
WHERE u.status = 'ativo'
GROUP BY u.nome, o.capacidade_kg
ORDER BY ocupacao_pct DESC;


-- 4) Alimentos resgatados por categoria e mês (últimos 6 meses): base do relatório ESG.
--    agregação por duas dimensões · COUNT, SUM, date_trunc, GROUP BY
SELECT to_char(date_trunc('month', l.entregue_em), 'YYYY-MM') AS mes,
       l.categoria,
       COUNT(*)                              AS lotes,
       ROUND(SUM(l.peso_kg), 1)              AS kg
FROM lotes l
WHERE l.estado = 'entregue'
  AND l.entregue_em >= date_trunc('month', now()) - INTERVAL '5 months'
GROUP BY 1, l.categoria
ORDER BY mes DESC, kg DESC;


-- 5) Qualidade das entregas: divergência média entre o peso declarado pela empresa
--    na retirada e o peso conferido pela ONG, por par empresa → ONG.
--    JOIN triplo em usuarios (empresa e ONG) · AVG, MAX, COUNT com FILTER
SELECT ue.nome                               AS empresa,
       uo.nome                               AS ong,
       COUNT(*)                              AS entregas_conferidas,
       ROUND(AVG(ABS(l.conf_div_pct)), 1)    AS divergencia_media_pct,
       ROUND(MAX(ABS(l.conf_div_pct)), 1)    AS maior_divergencia_pct,
       COUNT(*) FILTER (WHERE l.conf_condicao <> 'ok') AS recebimentos_com_problema
FROM lotes l
JOIN usuarios ue ON ue.id = l.empresa_id
JOIN usuarios uo ON uo.id = l.ong_id
WHERE l.estado = 'entregue' AND l.conf_kg IS NOT NULL
GROUP BY ue.nome, uo.nome
ORDER BY divergencia_media_pct DESC;


-- 6) Saldo da carteira de cada motorista: fretes disponíveis, fretes retidos por
--    ocorrência e saques já pagos.
--    JOIN usuarios + movimentos · SUM com CASE, GROUP BY
SELECT u.nome                                AS motorista,
       ROUND(SUM(CASE WHEN mv.tipo = 'frete' AND mv.status = 'disponivel' THEN mv.valor ELSE 0 END), 2) AS fretes_disponiveis_rs,
       ROUND(SUM(CASE WHEN mv.tipo = 'frete' AND mv.status = 'retido'     THEN mv.valor ELSE 0 END), 2) AS fretes_retidos_rs,
       ROUND(SUM(CASE WHEN mv.tipo = 'saque'                              THEN ABS(mv.valor) ELSE 0 END), 2) AS saques_rs,
       COUNT(*)                              AS movimentos
FROM usuarios u
JOIN movimentos mv ON mv.motorista_id = u.id
GROUP BY u.nome
ORDER BY fretes_disponiveis_rs DESC;


-- 7) Ocorrências abertas com o contexto completo para o administrador analisar:
--    lote, empresa, ONG, motorista e frete retido.
--    JOIN ocorrencias + lotes + 3x usuarios (LEFT JOIN no motorista) · sem agregação
SELECT oc.id                                 AS ocorrencia,
       oc.tipo,
       oc.criado_em::date                    AS aberta_em,
       l.id                                  AS lote,
       ue.nome                               AS empresa,
       uo.nome                               AS ong,
       um.nome                               AS motorista,
       l.frete                               AS frete_rs,
       l.frete_retido
FROM ocorrencias oc
JOIN lotes l          ON l.id  = oc.lote_id
JOIN usuarios ue      ON ue.id = l.empresa_id
JOIN usuarios uo      ON uo.id = l.ong_id
LEFT JOIN usuarios um ON um.id = l.motorista_id
WHERE oc.status = 'aberta'
ORDER BY oc.criado_em;
