package br.com.ongsave.web;

import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Enumeration;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import br.com.ongsave.config.Configuracao;
import br.com.ongsave.dao.LoteDAO;
import br.com.ongsave.dao.NotificacaoDAO;
import br.com.ongsave.dao.PosicaoDAO;
import br.com.ongsave.dao.UsuarioDAO;
import br.com.ongsave.db.Banco;
import br.com.ongsave.db.Migrador;
import br.com.ongsave.db.PoolConexoes;
import br.com.ongsave.db.Sql;
import br.com.ongsave.model.Conta;
import br.com.ongsave.util.Senhas;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;

/**
 * Arranque da aplicação:
 * <ol>
 *   <li>lê a configuração (WEB-INF/ongsave.properties ou variáveis de ambiente);</li>
 *   <li>abre o pool de conexões ao Neon e testa a ligação;</li>
 *   <li>aplica as migrações SQL pendentes (cria as tabelas na primeira vez);</li>
 *   <li>cria os serviços e agenda a manutenção periódica.</li>
 * </ol>
 * Se algo falhar, a aplicação continua de pé e o {@link SegurancaFilter} mostra uma página explicativa
 * em vez de erros 500 soltos.
 */
@WebListener
public class Inicializador implements ServletContextListener {

    /** Mensagem de erro de arranque (atributo do contexto), lida pelo filtro. */
    public static final String ERRO = "ongsave.erroArranque";
    private static final Logger LOG = Logger.getLogger(Inicializador.class.getName());

    private ScheduledExecutorService agenda;
    private br.com.ongsave.service.Simulador simulador;

    @Override
    public void contextInitialized(ServletContextEvent e) {
        ServletContext ctx = e.getServletContext();
        Configuracao cfg;
        try {
            cfg = Configuracao.carregar(ctx);
        } catch (IllegalStateException ex) {
            falhar(ctx, ex.getMessage(), ex);
            return;
        }
        ctx.setAttribute(Configuracao.ATRIBUTO, cfg);

        if (!cfg.bancoConfigurado()) {
            falhar(ctx, "Banco de dados não configurado. Abra src/main/webapp/WEB-INF/ongsave.properties e cole a "
                    + "connection string do Neon em db.url (ou defina a variável de ambiente DATABASE_URL).", null);
            return;
        }
        try {
            Class.forName("org.postgresql.Driver");
        } catch (ClassNotFoundException ex) {
            falhar(ctx, "Driver PostgreSQL não encontrado. Coloque o postgresql-42.7.13.jar em src/main/webapp/WEB-INF/lib (Eclipse) "
                    + "(execute baixar-libs.bat ou baixar-libs.sh na raiz do projeto).", ex);
            return;
        }

        PoolConexoes pool = new PoolConexoes(cfg.getJdbcUrl(), cfg.getUsuarioBanco(), cfg.getSenhaBanco(), cfg.poolMaximo());
        Banco.iniciar(pool);
        try {
            LOG.info(() -> "OngSave: a ligar a " + cfg.jdbcUrlParaLog());
            String versao = Banco.ler(c -> (String) Sql.valor(c, "SHOW server_version"));
            LOG.info(() -> "OngSave: ligado ao PostgreSQL " + versao);
            new Migrador(ctx, cfg.dadosDemo()).migrar();
        } catch (Exception ex) {
            Banco.parar();
            falhar(ctx, "Não foi possível ligar ao banco de dados ou preparar as tabelas: " + causa(ex)
                    + ". Confira db.url, utilizador e senha do Neon.", ex);
            return;
        }

        trocarSenhaAdminInicial(cfg);
        Servicos servicos = new Servicos(cfg);
        ctx.setAttribute(Servicos.ATRIBUTO, servicos);
        ctx.removeAttribute(ERRO);
        if (servicos.simulador != null) {
            simulador = servicos.simulador;
            simulador.iniciar();
        }

        agenda = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "ongsave-manutencao");
            t.setDaemon(true);
            return t;
        });
        agenda.scheduleWithFixedDelay(this::manutencao, 2, 60, TimeUnit.MINUTES);
        LOG.info("OngSave: aplicação pronta.");
    }

    /** Tarefas periódicas: expira lotes vencidos e aplica a retenção de dados (LGPD). */
    private void manutencao() {
        try {
            Banco.transacao(c -> {
                new NotificacaoDAO().avisarLotesVencidos(c);
                new LoteDAO().cancelarVencidos(c);
                // Posições GPS só servem durante a entrega e para auditoria recente.
                new PosicaoDAO().apagarAntigas(c, 90);
                new NotificacaoDAO().apagarLidasAntigas(c, 180);
                return null;
            });
        } catch (RuntimeException ex) {
            LOG.log(Level.WARNING, "OngSave: falha na manutenção periódica", ex);
        }
    }

    /**
     * Se app.adminSenha estiver definida e o admin ainda usar a senha padrão de instalação ("ongsave123"),
     * troca-a no arranque. Assim a produção nunca fica com a senha conhecida.
     */
    private static void trocarSenhaAdminInicial(Configuracao cfg) {
        String nova = cfg.texto("app.adminSenha", "");
        if (nova.isEmpty()) return;
        if (nova.length() < 8) { LOG.warning("OngSave: app.adminSenha ignorada (mínimo de 8 caracteres)."); return; }
        try {
            int n = Banco.transacao(c -> {
                UsuarioDAO usuarios = new UsuarioDAO();
                Conta admin = usuarios.buscarPorEmail(c, "admin@ongsave.com", true);
                if (admin == null || !Senhas.confere("ongsave123", admin.getSenhaHash())) return 0;
                usuarios.atualizarSenha(c, admin.getId(), Senhas.gerar(nova));
                return 1;
            });
            if (n > 0) LOG.info("OngSave: senha inicial do administrador substituída pela de app.adminSenha.");
        } catch (RuntimeException ex) {
            LOG.log(Level.WARNING, "OngSave: não foi possível trocar a senha inicial do administrador", ex);
        }
    }

    @Override
    public void contextDestroyed(ServletContextEvent e) {
        if (agenda != null) agenda.shutdownNow();
        if (simulador != null) simulador.parar();
        Banco.parar();
        // Evita fuga de memória no redeploy do Tomcat: remove os drivers carregados por esta aplicação.
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        Enumeration<Driver> drivers = DriverManager.getDrivers();
        while (drivers.hasMoreElements()) {
            Driver d = drivers.nextElement();
            if (d.getClass().getClassLoader() == cl) {
                try { DriverManager.deregisterDriver(d); } catch (SQLException ex) { /* ignora */ }
            }
        }
    }

    private static void falhar(ServletContext ctx, String msg, Throwable causa) {
        ctx.setAttribute(ERRO, msg);
        if (causa == null) LOG.severe("OngSave: " + msg);
        else LOG.log(Level.SEVERE, "OngSave: " + msg, causa);
    }

    private static String causa(Throwable t) {
        Throwable r = t;
        while (r.getCause() != null && r.getCause() != r) r = r.getCause();
        String m = r.getMessage();
        return m == null ? r.getClass().getSimpleName() : m;
    }
}
