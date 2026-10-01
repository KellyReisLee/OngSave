package br.com.ongsave.dao;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import br.com.ongsave.db.Sql;
import br.com.ongsave.model.Empresa;

/** Acesso à tabela empresas (dados da empresa doadora). */
public class EmpresaDAO {

    public Empresa buscar(Connection c, long usuarioId) throws SQLException {
        Map<String, Object> m = Sql.linha(c, "SELECT usuario_id, setor, responsavel, plano, custo_descarte, cep, rua, numero, complemento, "
                + "bairro, cidade, uf, lat, lon FROM empresas WHERE usuario_id = ?", usuarioId);
        if (m == null) return null;
        Empresa e = new Empresa();
        e.setUsuarioId(Linhas.l(m, "usuario_id"));
        e.setSetor(Linhas.s(m, "setor"));
        e.setResponsavel(Linhas.s(m, "responsavel"));
        e.setPlano(Linhas.s(m, "plano"));
        e.setCustoDescarte(Linhas.dbl(m, "custo_descarte"));
        e.setCep(Linhas.s(m, "cep"));
        e.setRua(Linhas.s(m, "rua"));
        e.setNumero(Linhas.s(m, "numero"));
        e.setComplemento(Linhas.s(m, "complemento"));
        e.setBairro(Linhas.s(m, "bairro"));
        e.setCidade(Linhas.s(m, "cidade"));
        e.setUf(Linhas.s(m, "uf"));
        e.setLat(Linhas.dbl(m, "lat"));
        e.setLon(Linhas.dbl(m, "lon"));
        return e;
    }

    /** Perfil no formato do painel da empresa (campos com os nomes que o JavaScript usa). */
    public Map<String, Object> perfilPainel(Connection c, long usuarioId) throws SQLException {
        return Sql.linha(c, """
                SELECT u.nome, u.documento AS cnpj, u.email, COALESCE(u.telefone, '') AS tel,
                       COALESCE(e.cep, '') AS cep, COALESCE(e.rua, '') AS rua, COALESCE(e.numero, '') AS num,
                       COALESCE(e.complemento, '') AS compl, COALESCE(e.bairro, '') AS bairro,
                       COALESCE(e.cidade, 'São Paulo') AS cidade, COALESCE(e.uf, 'SP') AS uf,
                       COALESCE(e.custo_descarte::text, '') AS descarte, e.plano, e.setor, e.responsavel AS resp,
                       COALESCE(e.lat, -23.5505) AS lat, COALESCE(e.lon, -46.6333) AS lon, e.lat IS NULL AS "semLocal"
                FROM usuarios u JOIN empresas e ON e.usuario_id = u.id WHERE u.id = ?""", usuarioId);
    }

    /** Empresas ativas com o plano e o número de lotes do mês (receita contratada do admin). */
    public List<Map<String, Object>> listarAtivasComUsoDoMes(Connection c) throws SQLException {
        return Sql.lista(c, """
                SELECT e.plano, (SELECT count(*) FROM lotes l WHERE l.empresa_id = u.id AND l.estado <> 'cancelado'
                                 AND l.criado_em >= date_trunc('month', now())) AS uso
                FROM usuarios u JOIN empresas e ON e.usuario_id = u.id WHERE u.status = 'ativo'""");
    }

    /** Uma empresa ativa ao acaso (modo simulação); com preferirDemo, a conta empresa@ongsave.com vem primeiro. */
    public Map<String, Object> sortearAtiva(Connection c, boolean preferirDemo) throws SQLException {
        return Sql.linha(c, """
                SELECT u.id AS "empId", u.nome AS "empNome", COALESCE(e.lat, -23.5505) AS lat, COALESCE(e.lon, -46.6333) AS lon
                FROM usuarios u JOIN empresas e ON e.usuario_id = u.id
                WHERE u.perfil = 'empresa' AND u.status = 'ativo'
                ORDER BY CASE WHEN ? AND lower(u.email) = 'empresa@ongsave.com' THEN 0 ELSE 1 END, random() LIMIT 1""", preferirDemo);
    }

    /** Cadastro inicial (o resto do perfil é preenchido depois pela empresa). */
    public void inserirCadastro(Connection c, Empresa e) throws SQLException {
        Sql.executar(c, "INSERT INTO empresas (usuario_id, setor, responsavel) VALUES (?, ?, ?)", e.getUsuarioId(), e.getSetor(), e.getResponsavel());
    }

    /** Atualiza custo de descarte e endereço; lat/lon nulos mantêm as coordenadas atuais. */
    public void atualizarPerfil(Connection c, Empresa e) throws SQLException {
        Sql.executar(c, """
                UPDATE empresas SET custo_descarte = ?, cep = ?, rua = ?, numero = ?, complemento = ?, bairro = ?, cidade = ?, uf = ?,
                       lat = COALESCE(?, lat), lon = COALESCE(?, lon)
                WHERE usuario_id = ?""",
                e.getCustoDescarte(), e.getCep(), e.getRua(), e.getNumero(), e.getComplemento(), e.getBairro(), e.getCidade(), e.getUf(),
                e.getLat(), e.getLon(), e.getUsuarioId());
    }

    public void atualizarPlano(Connection c, long usuarioId, String plano) throws SQLException {
        Sql.executar(c, "UPDATE empresas SET plano = ? WHERE usuario_id = ?", plano, usuarioId);
    }
}
