package br.com.ongsave.db;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Ponto único de acesso ao banco.
 *
 * <pre>
 *   Banco.ler(c -> Sql.lista(c, "SELECT ...", id));                 // leitura (autocommit)
 *   Banco.transacao(c -> { Sql.executar(c, ...); return null; });   // escrita atómica
 * </pre>
 * Erros SQL viram {@link ErroBanco} (não verificada) e a transação é desfeita.
 */
public final class Banco {

    private static final Logger LOG = Logger.getLogger(Banco.class.getName());
    private static volatile PoolConexoes pool;

    private Banco() {}

    @FunctionalInterface
    public interface Trabalho<T> {
        T executar(Connection c) throws SQLException;
    }

    /** Falha de infraestrutura (rede, SQL inválido...). A mensagem NÃO vai para o utilizador. */
    public static final class ErroBanco extends RuntimeException {
        private static final long serialVersionUID = 1L;
        public ErroBanco(String msg, Throwable causa) { super(msg, causa); }
    }

    public static void iniciar(PoolConexoes p) { pool = p; }

    public static void parar() {
        PoolConexoes p = pool;
        pool = null;
        if (p != null) p.close();
    }

    public static boolean pronto() { return pool != null; }

    public static Connection conexao() throws SQLException {
        PoolConexoes p = pool;
        if (p == null) throw new SQLException("Banco de dados não inicializado. Veja WEB-INF/ongsave.properties.");
        return p.getConnection();
    }

    public static <T> T ler(Trabalho<T> t) {
        return executar(t, false);
    }

    public static <T> T transacao(Trabalho<T> t) {
        return executar(t, true);
    }

    private static <T> T executar(Trabalho<T> t, boolean transacao) {
        for (int tentativa = 1; ; tentativa++) {
            try (Connection c = conexao()) {
                if (!transacao) return t.executar(c);
                c.setAutoCommit(false);
                try {
                    T r = t.executar(c);
                    c.commit();
                    return r;
                } catch (SQLException | RuntimeException e) {
                    try { c.rollback(); } catch (SQLException ignorar) { /* conexão já perdida */ }
                    throw e;
                }
            } catch (SQLException e) {
                // Conexão caída (ex.: Neon acordando): uma nova tentativa é segura porque nada foi confirmado.
                if (tentativa == 1 && e.getSQLState() != null && e.getSQLState().startsWith("08")) {
                    LOG.log(Level.WARNING, "Conexão perdida, nova tentativa: {0}", e.getMessage());
                    continue;
                }
                throw new ErroBanco("Erro no banco de dados: " + e.getMessage(), e);
            }
        }
    }
}
