package br.com.ongsave.dao;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import br.com.ongsave.db.Sql;

/** Acesso às tabelas sensores e leituras_sensores (IoT simulado). */
public class SensorDAO {

    public Map<String, Object> buscar(Connection c, String id) throws SQLException {
        return Sql.linha(c, """
                SELECT s.id, s.tipo, s.usuario_id AS "donoId", u.nome AS dono, s.descricao, s.unidade,
                       s.limite_min AS "limiteMin", s.limite_max AS "limiteMax", s.ativo
                FROM sensores s JOIN usuarios u ON u.id = s.usuario_id
                WHERE s.id = ?""", id);
    }

    /** Sensores ativos com a última leitura e, para as balanças, o lote que está a ser preparado para a retirada. */
    public List<Map<String, Object>> listar(Connection c) throws SQLException {
        return Sql.lista(c, """
                SELECT s.id, s.tipo, s.descricao, s.unidade, u.nome AS dono,
                       s.limite_min AS "limiteMin", s.limite_max AS "limiteMax",
                       ult.valor AS "ultimoValor", ult.alerta AS "ultimoAlerta", ult.registada_em AS "ultimaLeitura",
                       lt.id AS "loteId", lt.peso_kg AS "loteKg"
                FROM sensores s
                JOIN usuarios u ON u.id = s.usuario_id
                LEFT JOIN LATERAL (SELECT valor, alerta, registada_em FROM leituras_sensores
                                   WHERE sensor_id = s.id ORDER BY registada_em DESC LIMIT 1) ult ON TRUE
                LEFT JOIN LATERAL (SELECT id, peso_kg FROM lotes
                                   WHERE s.tipo = 'peso' AND empresa_id = s.usuario_id AND estado IN ('aguardando', 'aceito')
                                   ORDER BY criado_em DESC LIMIT 1) lt ON TRUE
                WHERE s.ativo
                ORDER BY s.tipo, s.id""");
    }

    /** O lote é desta empresa e ainda não saiu da doca (aguardando ou aceito)? Devolve o peso declarado ou null. */
    public Map<String, Object> loteParaPesagem(Connection c, long loteId, long empresaId) throws SQLException {
        return Sql.linha(c, """
                SELECT id, peso_kg AS kg, ong_id AS "ongId" FROM lotes
                WHERE id = ? AND empresa_id = ? AND estado IN ('aguardando', 'aceito')""", loteId, empresaId);
    }

    /** A leitura anterior deste sensor já estava em alerta? (evita repetir a notificação a cada leitura) */
    public boolean ultimaEmAlerta(Connection c, String sensorId) throws SQLException {
        Object v = Sql.valor(c, "SELECT alerta FROM leituras_sensores WHERE sensor_id = ? ORDER BY registada_em DESC LIMIT 1", sensorId);
        return Boolean.TRUE.equals(v);
    }

    public long inserirLeitura(Connection c, String sensorId, Long loteId, double valor, boolean alerta, String motivo) throws SQLException {
        return Sql.inserir(c, "INSERT INTO leituras_sensores (sensor_id, lote_id, valor, alerta, motivo) VALUES (?, ?, ?, ?, ?)",
                sensorId, loteId, valor, alerta, motivo);
    }

    public List<Map<String, Object>> leituras(Connection c, String sensorId, int limite) throws SQLException {
        return Sql.lista(c, """
                SELECT id, lote_id AS "loteId", valor, alerta, motivo, registada_em AS em
                FROM leituras_sensores WHERE sensor_id = ? ORDER BY registada_em DESC LIMIT ?""", sensorId, limite);
    }
}
