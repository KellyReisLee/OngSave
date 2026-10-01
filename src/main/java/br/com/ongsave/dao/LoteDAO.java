package br.com.ongsave.dao;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import br.com.ongsave.db.Sql;
import br.com.ongsave.model.Lote;

/**
 * Acesso à tabela lotes.
 *
 * <p>Dois tipos de leitura:</p>
 * <ul>
 *   <li>{@link #buscarPorId} devolve a entidade {@link Lote}, usada pelas regras de negócio;</li>
 *   <li>os métodos {@code detalhes*} devolvem a "vista de lote" usada pelos painéis: uma linha plana com o lote,
 *       os nomes e coordenadas da empresa, da ONG e do motorista e a última posição GPS.</li>
 * </ul>
 */
public class LoteDAO {

    /** Colunas da entidade (sem as fotos, que são lidas à parte por {@link #lerFoto}). */
    private static final String COLUNAS = """
            id, empresa_id, ong_id, motorista_id, categoria, peso_kg, volumes, conservacao, veiculo, validade,
            janela_inicio, janela_fim, observacao, foto_tipo, estado, frete, criado_em, ong_aceite_em, aceito_em, cancelado_em,
            retirada_codigo, retirada_kg, retirada_temp, retirada_obs, retirada_em, coleta_em,
            token_codigo, token_gerado_em, token_expira_em, token_usado_em, token_tentativas,
            entrega_foto_tipo, entrega_lat, entrega_lon, entrega_dist_m, foto_capturada_em, entregue_em,
            conf_kg, conf_condicao, conf_obs, conf_div_pct, conf_em, frete_retido, simulado""";

    /** Vista de lote para os painéis (empresa, ONG, motorista, admin). */
    private static final String VISTA = """
            SELECT l.id, l.categoria AS tipo, l.peso_kg AS kg, l.volumes, l.conservacao AS cons, l.veiculo AS "veicKey",
                   l.estado, l.frete, l.observacao AS obs, l.frete_retido AS "freteRetido",
                   l.validade, EXTRACT(EPOCH FROM (l.validade - now())) / 3600.0 AS h,
                   (EXTRACT(EPOCH FROM l.criado_em) * 1000)::bigint AS "criadoMs",
                   (EXTRACT(EPOCH FROM l.entregue_em) * 1000)::bigint AS "entregueMs",
                   FLOOR(EXTRACT(EPOCH FROM (now() - COALESCE(l.entregue_em, l.criado_em))) / 86400)::int AS dias,
                   l.criado_em >= date_trunc('month', now()) AS "doMes",
                   l.empresa_id AS "empresaId", ue.nome AS emp,
                   COALESCE(e.lat, -23.5505) AS "empLat", COALESCE(e.lon, -46.6333) AS "empLon",
                   l.ong_id AS "ongId", uo.nome AS ong,
                   COALESCE(o.lat, -23.5505) AS "ongLat", COALESCE(o.lon, -46.6333) AS "ongLon",
                   l.ong_aceite_em IS NOT NULL AS "ongAceitou",
                   (EXTRACT(EPOCH FROM l.ong_aceite_em) * 1000)::bigint AS "ongAceiteMs",
                   l.motorista_id AS "motoristaId", um.nome AS mot, m.placa, m.veiculo AS "motVeiculo",
                   l.retirada_codigo AS "retCodigo", l.retirada_kg AS "retKg", l.retirada_temp AS "retTemp",
                   l.retirada_obs AS "retObs", (EXTRACT(EPOCH FROM l.retirada_em) * 1000)::bigint AS "retMs",
                   l.token_codigo AS "tokCodigo", (EXTRACT(EPOCH FROM l.token_gerado_em) * 1000)::bigint AS "tokMs",
                   (EXTRACT(EPOCH FROM l.token_expira_em) * 1000)::bigint AS "tokExpira", l.token_usado_em IS NOT NULL AS "tokUsado",
                   l.entrega_dist_m AS "fotoDist", l.entrega_foto IS NOT NULL AS "temFoto", l.foto IS NOT NULL AS "temFotoLote",
                   l.conf_kg AS "confKg", l.conf_condicao AS "confCond", l.conf_obs AS "confObs", l.conf_div_pct AS "confDiv",
                   p.lat AS "posLat", p.lon AS "posLon"
            FROM lotes l
            JOIN usuarios ue ON ue.id = l.empresa_id
            LEFT JOIN empresas e ON e.usuario_id = l.empresa_id
            LEFT JOIN usuarios uo ON uo.id = l.ong_id
            LEFT JOIN ongs o ON o.usuario_id = l.ong_id
            LEFT JOIN usuarios um ON um.id = l.motorista_id
            LEFT JOIN motoristas m ON m.usuario_id = l.motorista_id
            LEFT JOIN LATERAL (SELECT lat, lon FROM posicoes WHERE lote_id = l.id ORDER BY registado_em DESC LIMIT 1) p
                   ON l.estado IN ('aceito', 'transito')
            """;

    /* ============================ Entidade ============================ */

    public Lote buscarPorId(Connection c, long id, boolean paraAtualizar) throws SQLException {
        return lote(Sql.linha(c, "SELECT " + COLUNAS + " FROM lotes WHERE id = ?" + (paraAtualizar ? " FOR UPDATE" : ""), id));
    }

    /** Foto da publicação ou da prova de entrega. */
    public byte[] lerFoto(Connection c, long id, boolean entrega) throws SQLException {
        return Sql.bytes(c, "SELECT " + (entrega ? "entrega_foto" : "foto") + " FROM lotes WHERE id = ?", id);
    }

    /** Publica um lote novo e devolve o id. */
    public long inserir(Connection c, Lote l) throws SQLException {
        return Sql.inserir(c, """
                INSERT INTO lotes (empresa_id, ong_id, categoria, peso_kg, volumes, conservacao, veiculo, validade,
                                   janela_inicio, janela_fim, observacao, foto, foto_tipo, simulado)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)""",
                l.getEmpresaId(), l.getOngId(), l.getCategoria(), l.getPesoKg(), l.getVolumes(), l.getConservacao(), l.getVeiculo(),
                l.getValidade(), l.getJanelaInicio(), l.getJanelaFim(), l.getObservacao(), l.getFoto(), l.getFotoTipo(), l.isSimulado());
    }

    /* ============================ Ciclo de vida ============================ */

    public void marcarSimulado(Connection c, long id) throws SQLException {
        Sql.executar(c, "UPDATE lotes SET simulado = TRUE WHERE id = ?", id);
    }

    public void cancelar(Connection c, long id) throws SQLException {
        Sql.executar(c, "UPDATE lotes SET estado = 'cancelado', cancelado_em = now() WHERE id = ?", id);
    }

    public void cancelarSeAguardando(Connection c, long id) throws SQLException {
        Sql.executar(c, "UPDATE lotes SET estado = 'cancelado', cancelado_em = now() WHERE id = ? AND estado = 'aguardando'", id);
    }

    /** Encerra os lotes que venceram sem motorista. Devolve quantos foram cancelados. */
    public int cancelarVencidos(Connection c) throws SQLException {
        return Sql.executar(c, "UPDATE lotes SET estado = 'cancelado', cancelado_em = now() WHERE estado = 'aguardando' AND validade < now()");
    }

    public void aceitarPelaOng(Connection c, long id) throws SQLException {
        Sql.executar(c, "UPDATE lotes SET ong_aceite_em = now() WHERE id = ?", id);
    }

    public void registrarRecusa(Connection c, long id, long ongId) throws SQLException {
        Sql.executar(c, "INSERT INTO lote_recusas (lote_id, ong_id) VALUES (?, ?) ON CONFLICT DO NOTHING", id, ongId);
    }

    /** Passa a proposta para outra ONG (ou nenhuma), à espera de novo aceite. */
    public void reatribuirOng(Connection c, long id, Long ongId) throws SQLException {
        Sql.executar(c, "UPDATE lotes SET ong_id = ?, ong_aceite_em = NULL WHERE id = ?", ongId, id);
    }

    /** Lotes ainda sem motorista de uma ONG que deixou de estar ativa voltam a procurar ONG. */
    public void desvincularOngDosAguardando(Connection c, long ongId) throws SQLException {
        Sql.executar(c, "UPDATE lotes SET ong_id = NULL, ong_aceite_em = NULL WHERE ong_id = ? AND estado = 'aguardando'", ongId);
    }

    /**
     * Aceite atómico do motorista: só tem efeito se o lote ainda estiver livre, aceite pela ONG e dentro da validade.
     * @return 1 se este motorista ganhou o lote, 0 se outro chegou primeiro
     */
    public int atribuirMotorista(Connection c, long id, long motoristaId, double frete) throws SQLException {
        return Sql.executar(c, "UPDATE lotes SET estado = 'aceito', motorista_id = ?, aceito_em = now(), frete = ? "
                + "WHERE id = ? AND estado = 'aguardando' AND motorista_id IS NULL AND ong_aceite_em IS NOT NULL AND validade > now()",
                motoristaId, frete, id);
    }

    public void registrarRetirada(Connection c, long id, String codigo, double kg, Double temperatura, String obs) throws SQLException {
        Sql.executar(c, "UPDATE lotes SET retirada_codigo = ?, retirada_kg = ?, retirada_temp = ?, retirada_obs = ?, retirada_em = now() WHERE id = ?",
                codigo, kg, temperatura, obs, id);
    }

    public void marcarColetado(Connection c, long id) throws SQLException {
        Sql.executar(c, "UPDATE lotes SET estado = 'transito', coleta_em = now() WHERE id = ?", id);
    }

    /** Novo token de entrega (invalida o anterior). */
    public void gerarToken(Connection c, long id, String codigo, int minutos) throws SQLException {
        Sql.executar(c, "UPDATE lotes SET token_codigo = ?, token_gerado_em = now(), token_expira_em = now() + make_interval(mins => ?), "
                + "token_usado_em = NULL, token_tentativas = 0 WHERE id = ?", codigo, minutos, id);
    }

    public void registrarTentativaToken(Connection c, long id, int tentativas) throws SQLException {
        Sql.executar(c, "UPDATE lotes SET token_tentativas = ? WHERE id = ?", tentativas, id);
    }

    /** Excesso de tentativas: o token expira e a ONG tem de gerar outro. */
    public void expirarToken(Connection c, long id) throws SQLException {
        Sql.executar(c, "UPDATE lotes SET token_tentativas = 0, token_expira_em = now() WHERE id = ?", id);
    }

    /** Entrega validada com token + foto. */
    public void registrarEntrega(Connection c, long id, byte[] foto, String fotoTipo, Double lat, Double lon, Integer distM,
                                 Instant capturadaEm) throws SQLException {
        Sql.executar(c, """
                UPDATE lotes SET estado = 'entregue', entregue_em = now(), token_usado_em = now(), token_tentativas = 0,
                       entrega_foto = ?, entrega_foto_tipo = ?, entrega_lat = ?, entrega_lon = ?, entrega_dist_m = ?, foto_capturada_em = ?
                WHERE id = ?""", foto, fotoTipo, lat, lon, distM, capturadaEm, id);
    }

    public void registrarConferencia(Connection c, long id, double kg, String condicao, String obs, double divergenciaPct,
                                     boolean freteRetido) throws SQLException {
        Sql.executar(c, "UPDATE lotes SET conf_kg = ?, conf_condicao = ?, conf_obs = ?, conf_div_pct = ?, conf_em = now(), frete_retido = ? WHERE id = ?",
                kg, condicao, obs, divergenciaPct, freteRetido, id);
    }

    public void liberarFrete(Connection c, long id) throws SQLException {
        Sql.executar(c, "UPDATE lotes SET frete_retido = FALSE WHERE id = ?", id);
    }

    /* ============================ Contagens ============================ */

    /** Id do lote em andamento do motorista (aceito ou em trânsito), ou null. */
    public Long idAtivoDoMotorista(Connection c, long motoristaId) throws SQLException {
        Object v = Sql.valor(c, "SELECT id FROM lotes WHERE motorista_id = ? AND estado IN ('aceito','transito')", motoristaId);
        return v == null ? null : ((Number) v).longValue();
    }

    public boolean motoristaTemEntregaAtiva(Connection c, long motoristaId) throws SQLException {
        return Sql.numero(c, "SELECT count(*) FROM lotes WHERE motorista_id = ? AND estado IN ('aceito','transito')", motoristaId) > 0;
    }

    /** Lotes da empresa no mês corrente (franquia do plano). */
    public long contarDoMes(Connection c, long empresaId) throws SQLException {
        return Sql.numero(c, "SELECT count(*) FROM lotes WHERE empresa_id = ? AND estado <> 'cancelado' "
                + "AND criado_em >= date_trunc('month', now())", empresaId);
    }

    public long contarSimuladosEmCurso(Connection c) throws SQLException {
        return Sql.numero(c, "SELECT count(*) FROM lotes WHERE simulado AND estado IN ('aguardando','aceito','transito')");
    }

    /* ============================ Vistas para os painéis ============================ */

    public List<Map<String, Object>> detalhesDaEmpresa(Connection c, long empresaId) throws SQLException {
        return Sql.lista(c, VISTA + " WHERE l.empresa_id = ? ORDER BY l.criado_em DESC LIMIT 300", empresaId);
    }

    /** Doações da ONG já com motorista (a caminho ou recebidas). */
    public List<Map<String, Object>> detalhesDaOng(Connection c, long ongId) throws SQLException {
        return Sql.lista(c, VISTA + " WHERE l.ong_id = ? AND l.estado IN ('aceito','transito','entregue') "
                + "ORDER BY COALESCE(l.entregue_em, l.aceito_em, l.criado_em) DESC LIMIT 300", ongId);
    }

    /** Propostas que esperam resposta (ou motorista) na ONG. */
    public List<Map<String, Object>> propostasDaOng(Connection c, long ongId) throws SQLException {
        return Sql.lista(c, VISTA + " WHERE l.ong_id = ? AND l.estado = 'aguardando' AND l.validade > now() ORDER BY l.validade LIMIT 100", ongId);
    }

    public Map<String, Object> detalheDaOng(Connection c, long id, long ongId) throws SQLException {
        return Sql.linha(c, VISTA + " WHERE l.id = ? AND l.ong_id = ?", id, ongId);
    }

    public List<Map<String, Object>> entreguesDoMotorista(Connection c, long motoristaId) throws SQLException {
        return Sql.lista(c, VISTA + " WHERE l.motorista_id = ? AND l.estado = 'entregue' ORDER BY l.entregue_em DESC LIMIT 500", motoristaId);
    }

    /** Lotes livres, já aceites pela ONG e dentro da validade (ofertas aos motoristas). */
    public List<Map<String, Object>> ofertas(Connection c) throws SQLException {
        return Sql.lista(c, VISTA + " WHERE l.estado = 'aguardando' AND l.motorista_id IS NULL AND l.ong_aceite_em IS NOT NULL "
                + "AND l.validade > now() ORDER BY l.validade LIMIT 200");
    }

    /** Entrega em andamento do motorista, ou null. */
    public Map<String, Object> detalheAtivoDoMotorista(Connection c, long motoristaId) throws SQLException {
        return Sql.linha(c, VISTA + " WHERE l.motorista_id = ? AND l.estado IN ('aceito','transito')", motoristaId);
    }

    /** Lotes publicados, em coleta e em trânsito (operações ao vivo do admin). */
    public List<Map<String, Object>> detalhesEmOperacao(Connection c) throws SQLException {
        return Sql.lista(c, VISTA + " WHERE (l.estado IN ('aceito','transito') OR (l.estado = 'aguardando' AND l.validade > now())) "
                + "ORDER BY l.criado_em DESC LIMIT 50");
    }

    /** Todas as entregas em andamento com motorista (usado pelo simulador). */
    public List<Map<String, Object>> detalhesEmAndamento(Connection c) throws SQLException {
        return Sql.lista(c, VISTA + " WHERE l.estado IN ('aceito','transito') AND l.motorista_id IS NOT NULL");
    }

    /* ============================ Estatísticas ============================ */

    /** Entregas da empresa (data, kg efetivo, ONG, categoria) para KPIs e ESG. */
    public List<Map<String, Object>> entregasDaEmpresa(Connection c, long empresaId) throws SQLException {
        return Sql.lista(c, "SELECT entregue_em, COALESCE(conf_kg, retirada_kg, peso_kg) AS kg, ong_id, categoria "
                + "FROM lotes WHERE empresa_id = ? AND estado = 'entregue' AND entregue_em IS NOT NULL", empresaId);
    }

    /** Recebimentos da ONG (data, kg efetivo, empresa, categoria). */
    public List<Map<String, Object>> entregasDaOng(Connection c, long ongId) throws SQLException {
        return Sql.lista(c, "SELECT entregue_em, COALESCE(conf_kg, retirada_kg, peso_kg) AS kg, empresa_id, categoria "
                + "FROM lotes WHERE ong_id = ? AND estado = 'entregue' AND entregue_em IS NOT NULL", ongId);
    }

    /** Empresas que mais doaram à ONG nos últimos 12 meses. */
    public List<Map<String, Object>> principaisDoadores(Connection c, long ongId) throws SQLException {
        return Sql.lista(c, "SELECT ue.nome, round(sum(COALESCE(l.conf_kg, l.retirada_kg, l.peso_kg))) AS kg "
                + "FROM lotes l JOIN usuarios ue ON ue.id = l.empresa_id WHERE l.ong_id = ? AND l.estado = 'entregue' "
                + "AND l.entregue_em > now() - interval '365 days' GROUP BY ue.nome ORDER BY 2 DESC LIMIT 6", ongId);
    }

    public List<Map<String, Object>> veiculosDaEmpresa(Connection c, long empresaId) throws SQLException {
        return Sql.lista(c, "SELECT veiculo AS k, count(*) AS n FROM lotes WHERE empresa_id = ? AND estado <> 'cancelado' "
                + "AND criado_em > now() - interval '90 days' GROUP BY veiculo", empresaId);
    }

    public List<Map<String, Object>> kgPorCategoriaDoMotorista(Connection c, long motoristaId) throws SQLException {
        return Sql.lista(c, "SELECT categoria, sum(COALESCE(conf_kg, peso_kg)) AS kg FROM lotes "
                + "WHERE motorista_id = ? AND estado = 'entregue' AND entregue_em > now() - interval '90 days' GROUP BY categoria ORDER BY 2 DESC",
                motoristaId);
    }

    /* ============================ Modo simulação ============================ */

    public List<Map<String, Object>> simuladosParaConferir(Connection c) throws SQLException {
        return Sql.lista(c, """
                SELECT l.id, l.ong_id, uo.nome, COALESCE(l.retirada_kg, l.peso_kg) AS kg, l.motorista_id
                FROM lotes l JOIN usuarios uo ON uo.id = l.ong_id
                WHERE l.simulado AND l.estado = 'entregue' AND l.conf_kg IS NULL AND l.entregue_em < now() - interval '10 seconds'
                LIMIT 5""");
    }

    public List<Map<String, Object>> simuladosAguardandoOng(Connection c) throws SQLException {
        return Sql.lista(c, """
                SELECT l.id, l.ong_id, uo.nome FROM lotes l JOIN usuarios uo ON uo.id = l.ong_id
                WHERE l.simulado AND l.estado = 'aguardando' AND l.ong_aceite_em IS NULL AND l.criado_em < now() - interval '6 seconds'
                LIMIT 5""");
    }

    public List<Map<String, Object>> simuladosAguardandoMotorista(Connection c) throws SQLException {
        return Sql.lista(c, """
                SELECT id, veiculo FROM lotes
                WHERE simulado AND estado = 'aguardando' AND motorista_id IS NULL AND ong_aceite_em < now() - interval '5 seconds'
                  AND validade > now() ORDER BY id LIMIT 5""");
    }

    /* ============================ Mapeamento ============================ */

    private static Lote lote(Map<String, Object> m) {
        if (m == null) return null;
        Lote l = new Lote();
        l.setId(Linhas.l(m, "id"));
        l.setEmpresaId(Linhas.l(m, "empresa_id"));
        l.setOngId(Linhas.lng(m, "ong_id"));
        l.setMotoristaId(Linhas.lng(m, "motorista_id"));
        l.setCategoria(Linhas.s(m, "categoria"));
        l.setPesoKg(Linhas.d(m, "peso_kg"));
        l.setVolumes(Linhas.i(m, "volumes"));
        l.setConservacao(Linhas.s(m, "conservacao"));
        l.setVeiculo(Linhas.s(m, "veiculo"));
        l.setValidade(Linhas.instante(m, "validade"));
        l.setJanelaInicio(Linhas.hora(m, "janela_inicio"));
        l.setJanelaFim(Linhas.hora(m, "janela_fim"));
        l.setObservacao(Linhas.s(m, "observacao"));
        l.setFotoTipo(Linhas.s(m, "foto_tipo"));
        l.setEstado(Linhas.s(m, "estado"));
        l.setFrete(Linhas.dbl(m, "frete"));
        l.setCriadoEm(Linhas.instante(m, "criado_em"));
        l.setOngAceiteEm(Linhas.instante(m, "ong_aceite_em"));
        l.setAceitoEm(Linhas.instante(m, "aceito_em"));
        l.setCanceladoEm(Linhas.instante(m, "cancelado_em"));
        l.setRetiradaCodigo(Linhas.s(m, "retirada_codigo"));
        l.setRetiradaKg(Linhas.dbl(m, "retirada_kg"));
        l.setRetiradaTemp(Linhas.dbl(m, "retirada_temp"));
        l.setRetiradaObs(Linhas.s(m, "retirada_obs"));
        l.setRetiradaEm(Linhas.instante(m, "retirada_em"));
        l.setColetaEm(Linhas.instante(m, "coleta_em"));
        l.setTokenCodigo(Linhas.s(m, "token_codigo"));
        l.setTokenGeradoEm(Linhas.instante(m, "token_gerado_em"));
        l.setTokenExpiraEm(Linhas.instante(m, "token_expira_em"));
        l.setTokenUsadoEm(Linhas.instante(m, "token_usado_em"));
        l.setTokenTentativas(Linhas.i(m, "token_tentativas"));
        l.setEntregaFotoTipo(Linhas.s(m, "entrega_foto_tipo"));
        l.setEntregaLat(Linhas.dbl(m, "entrega_lat"));
        l.setEntregaLon(Linhas.dbl(m, "entrega_lon"));
        l.setEntregaDistM(Linhas.inteiro(m, "entrega_dist_m"));
        l.setFotoCapturadaEm(Linhas.instante(m, "foto_capturada_em"));
        l.setEntregueEm(Linhas.instante(m, "entregue_em"));
        l.setConfKg(Linhas.dbl(m, "conf_kg"));
        l.setConfCondicao(Linhas.s(m, "conf_condicao"));
        l.setConfObs(Linhas.s(m, "conf_obs"));
        l.setConfDivPct(Linhas.dbl(m, "conf_div_pct"));
        l.setConfEm(Linhas.instante(m, "conf_em"));
        l.setFreteRetido(Linhas.b(m, "frete_retido"));
        l.setSimulado(Linhas.b(m, "simulado"));
        return l;
    }
}
