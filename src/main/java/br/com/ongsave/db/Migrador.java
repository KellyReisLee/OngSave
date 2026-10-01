package br.com.ongsave.db;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jakarta.servlet.ServletContext;

/**
 * Migrações versionadas (estilo Flyway, sem dependências).
 *
 * Lê /WEB-INF/db/V###__descricao.sql por ordem e aplica as que ainda não constam
 * em {@code schema_versao}. Cada ficheiro corre numa transação: ou entra inteiro ou nada.
 * Um advisory lock impede que duas instâncias migrem ao mesmo tempo.
 */
public final class Migrador {

    private static final Logger LOG = Logger.getLogger(Migrador.class.getName());
    private static final Pattern NOME = Pattern.compile("V(\\d+)__(.+)\\.sql");
    private static final long CHAVE_LOCK = 72_700_001L;
    private static final String MARCADOR_DEMO = "@@DEMO@@";

    private final ServletContext ctx;
    private final boolean dadosDemo;

    public Migrador(ServletContext ctx, boolean dadosDemo) {
        this.ctx = ctx;
        this.dadosDemo = dadosDemo;
    }

    /**
     * Aplica as migrações pendentes. Cada uma corre na sua transação com um advisory lock de transação,
     * o que funciona também com o endpoint "-pooler" do Neon (PgBouncer em modo transação) e com vários
     * Tomcats a arrancar ao mesmo tempo.
     */
    public void migrar() throws SQLException, IOException {
        List<String[]> arquivos = listar();
        if (arquivos.isEmpty()) throw new IOException("Nenhuma migração encontrada em /WEB-INF/db/");

        // Caminho rápido: se o banco já tem todas as versões, não cria nem insere nada.
        // A criação das tabelas e dos dados iniciais acontece UMA ÚNICA VEZ (na primeira ligação ao banco).
        if (bancoJaPreparado(arquivos)) {
            LOG.info(() -> "OngSave: banco já preparado (V" + arquivos.get(arquivos.size() - 1)[0]
                    + "). Nenhuma tabela ou dado foi criado neste arranque.");
            return;
        }
        LOG.info("OngSave: banco novo ou com migrações pendentes — a preparar (só acontece uma vez).");
        try (Connection c = Banco.conexao()) {
            c.setAutoCommit(false);
            try {
                Sql.valor(c, "SELECT pg_advisory_xact_lock(?)", CHAVE_LOCK);
                Sql.script(c, "CREATE TABLE IF NOT EXISTS schema_versao ("
                        + " versao INT PRIMARY KEY, descricao VARCHAR(200) NOT NULL,"
                        + " aplicada_em TIMESTAMPTZ NOT NULL DEFAULT now())");
                c.commit();
                for (String[] a : arquivos) {
                    int versao = Integer.parseInt(a[0]);
                    Sql.valor(c, "SELECT pg_advisory_xact_lock(?)", CHAVE_LOCK);
                    if (Sql.numero(c, "SELECT count(*) FROM schema_versao WHERE versao = ?", versao) > 0) { c.commit(); continue; }
                    String sql = ler(a[2]);
                    if (!dadosDemo && sql.contains(MARCADOR_DEMO)) sql = sql.substring(0, sql.indexOf(MARCADOR_DEMO));
                    LOG.info(() -> "OngSave: a aplicar migração V" + a[0] + " (" + a[1] + ")");
                    try {
                        Sql.script(c, sql);
                        Sql.executar(c, "INSERT INTO schema_versao (versao, descricao) VALUES (?, ?)", versao, a[1]);
                        c.commit();
                    } catch (SQLException e) {
                        c.rollback();
                        throw new SQLException("Falha na migração V" + a[0] + "__" + a[1] + ": " + e.getMessage(), e.getSQLState(), e);
                    }
                }
            } catch (SQLException | IOException | RuntimeException e) {
                try { c.rollback(); } catch (SQLException ignorar) { /* conexão perdida */ }
                throw e;
            } finally {
                c.setAutoCommit(true);
            }
        }
    }

    /** true se a tabela schema_versao existe e já contém todas as versões dos ficheiros (consulta só de leitura). */
    private boolean bancoJaPreparado(List<String[]> arquivos) throws SQLException {
        try (Connection c = Banco.conexao()) {
            Object tabela = Sql.valor(c, "SELECT to_regclass('public.schema_versao')::text");
            if (tabela == null) return false;
            Set<Integer> aplicadas = new java.util.HashSet<>();
            for (var linha : Sql.lista(c, "SELECT versao FROM schema_versao"))
                aplicadas.add(((Number) linha.get("versao")).intValue());
            for (String[] a : arquivos)
                if (!aplicadas.contains(Integer.parseInt(a[0]))) return false;
            return true;
        }
    }

    /** [versão, descrição, caminho], ordenados pela versão. */
    private List<String[]> listar() {
        Set<String> caminhos = ctx.getResourcePaths("/WEB-INF/db/");
        List<String[]> r = new ArrayList<>();
        if (caminhos == null) return r;
        for (String p : new TreeSet<>(caminhos)) {
            Matcher m = NOME.matcher(p.substring(p.lastIndexOf('/') + 1));
            if (m.matches()) r.add(new String[] { m.group(1), m.group(2).replace('_', ' '), p });
        }
        r.sort((a, b) -> Integer.compare(Integer.parseInt(a[0]), Integer.parseInt(b[0])));
        return r;
    }

    private String ler(String caminho) throws IOException {
        try (InputStream in = ctx.getResourceAsStream(caminho)) {
            if (in == null) throw new IOException("Migração não encontrada: " + caminho);
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
