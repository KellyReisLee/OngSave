package br.com.ongsave.dao;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import br.com.ongsave.db.Sql;
import br.com.ongsave.model.Ong;

/** Acesso à tabela ongs (instituições que recebem as doações). */
public class OngDAO {

    public Ong buscar(Connection c, long usuarioId) throws SQLException {
        Map<String, Object> m = Sql.linha(c, "SELECT usuario_id, razao_social, responsavel, familias, capacidade_kg, hora_inicio, hora_fim, "
                + "camara_fria, categorias, alvara_validade, cep, rua, numero, complemento, bairro, cidade, uf, lat, lon "
                + "FROM ongs WHERE usuario_id = ?", usuarioId);
        if (m == null) return null;
        Ong o = new Ong();
        o.setUsuarioId(Linhas.l(m, "usuario_id"));
        o.setRazaoSocial(Linhas.s(m, "razao_social"));
        o.setResponsavel(Linhas.s(m, "responsavel"));
        o.setFamilias(Linhas.i(m, "familias"));
        o.setCapacidadeKg(Linhas.d(m, "capacidade_kg"));
        o.setHoraInicio(Linhas.hora(m, "hora_inicio"));
        o.setHoraFim(Linhas.hora(m, "hora_fim"));
        o.setCamaraFria(Linhas.b(m, "camara_fria"));
        o.setCategorias(Linhas.textos(m, "categorias"));
        o.setAlvaraValidade(Linhas.data(m, "alvara_validade"));
        o.setCep(Linhas.s(m, "cep"));
        o.setRua(Linhas.s(m, "rua"));
        o.setNumero(Linhas.s(m, "numero"));
        o.setComplemento(Linhas.s(m, "complemento"));
        o.setBairro(Linhas.s(m, "bairro"));
        o.setCidade(Linhas.s(m, "cidade"));
        o.setUf(Linhas.s(m, "uf"));
        o.setLat(Linhas.dbl(m, "lat"));
        o.setLon(Linhas.dbl(m, "lon"));
        return o;
    }

    /** Perfil no formato do painel da ONG. */
    public Map<String, Object> perfilPainel(Connection c, long usuarioId) throws SQLException {
        return Sql.linha(c, """
                SELECT COALESCE(o.razao_social, u.nome) AS nome, u.nome AS fantasia, COALESCE(u.documento, '') AS cnpj,
                       COALESCE(o.responsavel, '') AS resp, o.familias, o.capacidade_kg AS "capKg",
                       o.hora_inicio AS "horaIni", o.hora_fim AS "horaFim", o.camara_fria AS "camaraFria", o.categorias,
                       o.alvara_validade AS alvara, (o.alvara_validade - current_date) AS "alvaraDias",
                       u.email, COALESCE(u.telefone, '') AS tel, u.status,
                       COALESCE(o.cep, '') AS cep, COALESCE(o.rua, '') AS rua, COALESCE(o.numero, '') AS num,
                       COALESCE(o.complemento, '') AS compl, COALESCE(o.bairro, '') AS bairro,
                       COALESCE(o.cidade, 'São Paulo') AS cidade, COALESCE(o.uf, 'SP') AS uf,
                       COALESCE(o.lat, -23.5505) AS lat, COALESCE(o.lon, -46.6333) AS lon, o.lat IS NULL AS "semLocal"
                FROM usuarios u JOIN ongs o ON o.usuario_id = u.id WHERE u.id = ?""", usuarioId);
    }

    /** ONGs ativas com coordenadas (lista e mapa da empresa). */
    public List<Map<String, Object>> listarAtivas(Connection c) throws SQLException {
        return Sql.lista(c, "SELECT u.id, u.nome, COALESCE(o.lat, -23.5505) AS lat, COALESCE(o.lon, -46.6333) AS lon "
                + "FROM usuarios u JOIN ongs o ON o.usuario_id = u.id WHERE u.status = 'ativo' ORDER BY u.nome");
    }

    /**
     * ONGs ativas que aceitam a categoria, têm câmara fria quando o lote precisa e ainda não recusaram este lote.
     * @param loteRecusado lote para excluir quem já o recusou (null num lote novo)
     */
    public List<Map<String, Object>> listarCompativeis(Connection c, String categoria, String conservacao, Long loteRecusado) throws SQLException {
        return Sql.lista(c, """
                SELECT u.id, COALESCE(o.lat, -23.5505) AS lat, COALESCE(o.lon, -46.6333) AS lon
                FROM usuarios u JOIN ongs o ON o.usuario_id = u.id
                WHERE u.status = 'ativo' AND ? = ANY(o.categorias) AND (? = 'amb' OR o.camara_fria)
                  AND NOT EXISTS (SELECT 1 FROM lote_recusas r WHERE r.lote_id = ? AND r.ong_id = u.id)""",
                categoria, conservacao, loteRecusado == null ? -1L : loteRecusado);
    }

    /** Até 3 ONGs compatíveis com capacidade livre hoje para mais kg (modo simulação). */
    public List<Map<String, Object>> sortearComCapacidade(Connection c, String categoria, String conservacao, double kg,
                                                         boolean preferirDemo) throws SQLException {
        return Sql.lista(c, """
                SELECT u.id, COALESCE(o.lat, -23.5505) AS lat, COALESCE(o.lon, -46.6333) AS lon,
                       lower(u.email) = 'ong@ongsave.com' AS demo
                FROM usuarios u JOIN ongs o ON o.usuario_id = u.id
                WHERE u.perfil = 'ong' AND u.status = 'ativo' AND ? = ANY(o.categorias) AND (? = 'amb' OR o.camara_fria)
                  AND o.capacidade_kg - (SELECT COALESCE(sum(peso_kg), 0) FROM lotes x WHERE x.ong_id = u.id AND (
                        x.estado IN ('aceito','transito') OR (x.estado = 'aguardando' AND x.ong_aceite_em IS NOT NULL)
                        OR (x.estado = 'entregue' AND x.entregue_em >= date_trunc('day', now())))) >= ?
                ORDER BY CASE WHEN ? AND lower(u.email) = 'ong@ongsave.com' THEN 0 ELSE 1 END, random() LIMIT 3""",
                categoria, conservacao, kg, preferirDemo);
    }

    /** kg já comprometidos hoje: aceites a caminho, aceites à espera de motorista e entregues hoje. */
    public double kgPrevistoHoje(Connection c, long ongId) throws SQLException {
        return Sql.decimal(c, "SELECT COALESCE(sum(peso_kg), 0) FROM lotes WHERE ong_id = ? AND ("
                + "estado IN ('aceito','transito') OR (estado = 'aguardando' AND ong_aceite_em IS NOT NULL) "
                + "OR (estado = 'entregue' AND entregue_em >= date_trunc('day', now())))", ongId);
    }

    public void inserirCadastro(Connection c, Ong o) throws SQLException {
        Sql.executar(c, "INSERT INTO ongs (usuario_id, razao_social, responsavel, familias, alvara_validade) VALUES (?, ?, ?, ?, ?)",
                o.getUsuarioId(), o.getRazaoSocial(), o.getResponsavel(), o.getFamilias(), o.getAlvaraValidade());
    }

    /** Atualiza o perfil completo; lat/lon nulos mantêm as coordenadas atuais. */
    public void atualizarPerfil(Connection c, Ong o) throws SQLException {
        Sql.executar(c, """
                UPDATE ongs SET razao_social = ?, responsavel = ?, familias = ?, capacidade_kg = ?, hora_inicio = ?, hora_fim = ?,
                       camara_fria = ?, categorias = ?, alvara_validade = ?, cep = ?, rua = ?, numero = ?, complemento = ?,
                       bairro = ?, cidade = ?, uf = ?, lat = COALESCE(?, lat), lon = COALESCE(?, lon)
                WHERE usuario_id = ?""",
                o.getRazaoSocial(), o.getResponsavel(), o.getFamilias(), o.getCapacidadeKg(), o.getHoraInicio(), o.getHoraFim(),
                o.isCamaraFria(), Sql.arrayTexto(c, o.getCategorias()), o.getAlvaraValidade(), o.getCep(), o.getRua(), o.getNumero(),
                o.getComplemento(), o.getBairro(), o.getCidade(), o.getUf(), o.getLat(), o.getLon(), o.getUsuarioId());
    }
}
