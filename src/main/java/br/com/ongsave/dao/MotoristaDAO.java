package br.com.ongsave.dao;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import br.com.ongsave.db.Sql;
import br.com.ongsave.model.Motorista;

/** Acesso à tabela motoristas (veículo, CNH, endereço e consentimento LGPD). */
public class MotoristaDAO {

    public Motorista buscar(Connection c, long usuarioId, boolean paraAtualizar) throws SQLException {
        Map<String, Object> m = Sql.linha(c, "SELECT usuario_id, veiculo, placa, modelo, cnh, cnh_categoria, cnh_validade, cep, rua, numero, "
                + "bairro, cidade, uf, consentimento_em FROM motoristas WHERE usuario_id = ?" + (paraAtualizar ? " FOR UPDATE" : ""), usuarioId);
        if (m == null) return null;
        Motorista o = new Motorista();
        o.setUsuarioId(Linhas.l(m, "usuario_id"));
        o.setVeiculo(Linhas.s(m, "veiculo"));
        o.setPlaca(Linhas.s(m, "placa"));
        o.setModelo(Linhas.s(m, "modelo"));
        o.setCnh(Linhas.s(m, "cnh"));
        o.setCnhCategoria(Linhas.s(m, "cnh_categoria"));
        o.setCnhValidade(Linhas.data(m, "cnh_validade"));
        o.setCep(Linhas.s(m, "cep"));
        o.setRua(Linhas.s(m, "rua"));
        o.setNumero(Linhas.s(m, "numero"));
        o.setBairro(Linhas.s(m, "bairro"));
        o.setCidade(Linhas.s(m, "cidade"));
        o.setUf(Linhas.s(m, "uf"));
        o.setConsentimentoEm(Linhas.instante(m, "consentimento_em"));
        return o;
    }

    /** Perfil no formato do painel do motorista. */
    public Map<String, Object> perfilPainel(Connection c, long usuarioId) throws SQLException {
        return Sql.linha(c, """
                SELECT u.nome, COALESCE(u.documento, '') AS cpf, u.email, COALESCE(u.telefone, '') AS tel, u.status,
                       m.veiculo, COALESCE(m.placa, '') AS placa, COALESCE(m.modelo, '') AS modelo, COALESCE(m.cnh, '') AS cnh,
                       COALESCE(m.cnh_categoria, 'B') AS cat, m.cnh_validade AS "cnhValidade",
                       COALESCE(m.cep, '') AS cep, COALESCE(m.rua, '') AS rua, COALESCE(m.numero, '') AS num,
                       COALESCE(m.bairro, '') AS bairro, COALESCE(m.cidade, '') AS cidade, COALESCE(m.uf, '') AS uf,
                       m.consentimento_em AS consentimento
                FROM usuarios u JOIN motoristas m ON m.usuario_id = u.id WHERE u.id = ?""", usuarioId);
    }

    /** Motoristas ativos com acesso nos últimos 7 dias. */
    public long contarAtivosNaSemana(Connection c) throws SQLException {
        return Sql.numero(c, "SELECT count(DISTINCT m.usuario_id) FROM motoristas m JOIN usuarios u "
                + "ON u.id = m.usuario_id WHERE u.status = 'ativo' AND u.ultimo_acesso > now() - interval '7 days'");
    }

    /** Motoristas ativos, com CNH válida e sem entrega em andamento (modo simulação), em ordem aleatória. */
    public List<Map<String, Object>> listarLivres(Connection c) throws SQLException {
        return Sql.lista(c, """
                SELECT u.id, u.nome, m.veiculo FROM usuarios u JOIN motoristas m ON m.usuario_id = u.id
                WHERE u.perfil = 'motorista' AND u.status = 'ativo' AND (m.cnh_validade IS NULL OR m.cnh_validade >= current_date)
                  AND NOT EXISTS (SELECT 1 FROM lotes x WHERE x.motorista_id = u.id AND x.estado IN ('aceito','transito'))
                ORDER BY random()""");
    }

    public void inserirCadastro(Connection c, Motorista m) throws SQLException {
        Sql.executar(c, "INSERT INTO motoristas (usuario_id, placa, cnh, cnh_categoria, cnh_validade) VALUES (?, ?, ?, ?, ?)",
                m.getUsuarioId(), m.getPlaca(), m.getCnh(), m.getCnhCategoria(), m.getCnhValidade());
    }

    public void atualizarPerfil(Connection c, Motorista m) throws SQLException {
        Sql.executar(c, """
                UPDATE motoristas SET veiculo = ?, placa = ?, modelo = ?, cnh = ?, cnh_categoria = ?, cnh_validade = ?,
                       cep = ?, rua = ?, numero = ?, bairro = ?, cidade = ?, uf = ? WHERE usuario_id = ?""",
                m.getVeiculo(), m.getPlaca(), m.getModelo(), m.getCnh(), m.getCnhCategoria(), m.getCnhValidade(),
                m.getCep(), m.getRua(), m.getNumero(), m.getBairro(), m.getCidade(), m.getUf(), m.getUsuarioId());
    }

    /** Concede (agora) ou revoga o consentimento de localização. */
    public void definirConsentimento(Connection c, long usuarioId, boolean conceder) throws SQLException {
        Sql.executar(c, "UPDATE motoristas SET consentimento_em = " + (conceder ? "now()" : "NULL") + " WHERE usuario_id = ?", usuarioId);
    }

    /** Usado pelo simulador: motoristas automáticos dão o consentimento uma única vez. */
    public void concederConsentimentoSeFaltar(Connection c, long usuarioId) throws SQLException {
        Sql.executar(c, "UPDATE motoristas SET consentimento_em = now() WHERE usuario_id = ? AND consentimento_em IS NULL", usuarioId);
    }
}
