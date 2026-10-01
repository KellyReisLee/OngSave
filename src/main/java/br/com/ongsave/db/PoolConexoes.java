package br.com.ongsave.db;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.SQLTransientConnectionException;
import java.util.Iterator;
import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Pool de conexões JDBC leve, pensado para o Neon (PostgreSQL serverless).
 *
 * - O Neon suspende o compute após alguns minutos parado e derruba as conexões:
 *   conexões ociosas há mais de {@code maxOciosoMs} são descartadas e as que
 *   ficaram paradas mais de 30 s são validadas antes de serem entregues.
 * - Conexões com erro de rede (SQLState 08xxx) nunca voltam para o pool.
 * - {@code close()} na conexão devolve-a ao pool (use sempre try-with-resources).
 */
public final class PoolConexoes implements AutoCloseable {

    private static final Logger LOG = Logger.getLogger(PoolConexoes.class.getName());
    private static final long VALIDAR_SE_PARADA_MS = 30_000;

    private final String url;
    private final Properties propriedades;
    private final int maximo;
    private final long maxVidaMs;
    private final long maxOciosoMs;
    private final long esperaMs;

    private final LinkedBlockingDeque<Fisica> ociosas = new LinkedBlockingDeque<>();
    private final Semaphore vagas;
    private final ScheduledExecutorService limpeza;
    private volatile boolean fechado;

    private static final class Fisica {
        final Connection conexao;
        final long criadaEm = System.currentTimeMillis();
        volatile long devolvidaEm = System.currentTimeMillis();
        volatile boolean quebrada;

        Fisica(Connection c) { this.conexao = c; }
    }

    public PoolConexoes(String url, String usuario, String senha, int maximo) {
        this.url = url;
        this.propriedades = new Properties();
        if (usuario != null && !usuario.isBlank()) propriedades.setProperty("user", usuario);
        if (senha != null && !senha.isBlank()) propriedades.setProperty("password", senha);
        this.maximo = maximo;
        this.maxVidaMs = TimeUnit.MINUTES.toMillis(20);
        this.maxOciosoMs = TimeUnit.MINUTES.toMillis(4);
        this.esperaMs = TimeUnit.SECONDS.toMillis(20);
        this.vagas = new Semaphore(maximo, true);
        this.limpeza = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "ongsave-pool-limpeza");
            t.setDaemon(true);
            return t;
        });
        limpeza.scheduleWithFixedDelay(this::descartarVelhas, 60, 60, TimeUnit.SECONDS);
    }

    public Connection getConnection() throws SQLException {
        if (fechado) throw new SQLException("O pool de conexões foi encerrado");
        boolean temVaga;
        try {
            temVaga = vagas.tryAcquire(esperaMs, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new SQLException("Interrompido à espera de conexão", e);
        }
        if (!temVaga)
            throw new SQLTransientConnectionException("Todas as " + maximo + " conexões estão ocupadas há mais de "
                    + (esperaMs / 1000) + " s. Aumente db.pool.maximo ou procure transações demoradas.");
        try {
            Fisica f;
            while ((f = ociosas.pollFirst()) != null) {
                if (expirada(f) || !valida(f)) { fecharFisica(f); continue; }
                return embrulhar(f);
            }
            return embrulhar(abrir());
        } catch (SQLException | RuntimeException e) {
            vagas.release();
            throw e;
        }
    }

    private Fisica abrir() throws SQLException {
        Connection c = DriverManager.getConnection(url, propriedades);
        c.setAutoCommit(true);
        return new Fisica(c);
    }

    private boolean expirada(Fisica f) {
        long agora = System.currentTimeMillis();
        return agora - f.criadaEm > maxVidaMs || agora - f.devolvidaEm > maxOciosoMs;
    }

    private boolean valida(Fisica f) {
        if (System.currentTimeMillis() - f.devolvidaEm < VALIDAR_SE_PARADA_MS) return true;
        try {
            return f.conexao.isValid(5);
        } catch (SQLException e) {
            return false;
        }
    }

    private void devolver(Fisica f) {
        try {
            if (f.quebrada || f.conexao.isClosed()) {
                fecharFisica(f);
                return;
            }
            if (!f.conexao.getAutoCommit()) {
                f.conexao.rollback();          // transação esquecida aberta: nunca deixa lixo
                f.conexao.setAutoCommit(true);
            }
            f.conexao.clearWarnings();
            f.devolvidaEm = System.currentTimeMillis();
            if (fechado) fecharFisica(f);
            else ociosas.offerFirst(f);         // LIFO: reaproveita as conexões "quentes"
        } catch (SQLException e) {
            fecharFisica(f);
        } finally {
            vagas.release();
        }
    }

    private Connection embrulhar(Fisica f) {
        InvocationHandler h = new InvocationHandler() {
            private boolean devolvida;

            @Override
            public Object invoke(Object proxy, Method m, Object[] args) throws Throwable {
                switch (m.getName()) {
                    case "close":
                        if (!devolvida) { devolvida = true; devolver(f); }
                        return null;
                    case "isClosed":
                        return devolvida || f.conexao.isClosed();
                    case "equals":
                        return proxy == args[0];
                    case "hashCode":
                        return System.identityHashCode(proxy);
                    case "toString":
                        return "ConexaoOngSave[" + f.conexao + "]";
                    default:
                        break;
                }
                if (devolvida) throw new SQLException("Esta conexão já foi devolvida ao pool");
                try {
                    return m.invoke(f.conexao, args);
                } catch (InvocationTargetException e) {
                    Throwable causa = e.getCause();
                    if (causa instanceof SQLException s && s.getSQLState() != null && s.getSQLState().startsWith("08"))
                        f.quebrada = true;
                    throw causa;
                }
            }
        };
        return (Connection) Proxy.newProxyInstance(Connection.class.getClassLoader(), new Class<?>[] { Connection.class }, h);
    }

    private void descartarVelhas() {
        for (Iterator<Fisica> it = ociosas.iterator(); it.hasNext();) {
            Fisica f = it.next();
            if (expirada(f) && ociosas.remove(f)) fecharFisica(f);
        }
    }

    private static void fecharFisica(Fisica f) {
        try {
            f.conexao.close();
        } catch (SQLException e) {
            LOG.log(Level.FINE, "Erro ao fechar conexão física", e);
        }
    }

    public int emUso() { return maximo - vagas.availablePermits(); }

    @Override
    public void close() {
        fechado = true;
        limpeza.shutdownNow();
        Fisica f;
        while ((f = ociosas.pollFirst()) != null) fecharFisica(f);
    }
}
