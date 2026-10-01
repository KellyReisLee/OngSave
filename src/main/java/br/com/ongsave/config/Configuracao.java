package br.com.ongsave.config;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;
import java.util.StringJoiner;

import jakarta.servlet.ServletContext;

/**
 * Configuração da aplicação.
 *
 * Ordem de prioridade (a primeira que tiver valor ganha):
 *   1. Variáveis de ambiente   (DATABASE_URL, DB_USUARIO, DB_SENHA, ONGSAVE_GEO_VALIDAR ...)
 *   2. Propriedades da JVM      (-Ddb.url=...)
 *   3. /WEB-INF/ongsave.properties
 *
 * A URL aceita diretamente a connection string do Neon
 * ({@code postgresql://utilizador:senha@ep-xxx.neon.tech/neondb?sslmode=require})
 * ou uma URL JDBC ({@code jdbc:postgresql://...}).
 */
public final class Configuracao {

    public static final String ATRIBUTO = "ongsave.configuracao";
    private static final String ARQUIVO = "/WEB-INF/ongsave.properties";

    private final Properties props;

    private String jdbcUrl;
    private String usuarioBanco;
    private String senhaBanco;

    private Configuracao(Properties props) {
        this.props = props;
        montarConexao();
    }

    public static Configuracao carregar(ServletContext ctx) {
        Properties p = new Properties();
        try (InputStream in = ctx.getResourceAsStream(ARQUIVO)) {
            if (in != null) p.load(new InputStreamReader(in, StandardCharsets.UTF_8));
        } catch (IOException e) {
            ctx.log("OngSave: não foi possível ler " + ARQUIVO, e);
        }
        return new Configuracao(p);
    }

    /* ======================= Leitura de valores ======================= */

    /** Procura em variável de ambiente (ex.: db.url -> DB_URL), propriedade da JVM e depois no ficheiro. */
    public String texto(String chave, String padrao) {
        String env = System.getenv(chave.toUpperCase().replace('.', '_'));
        if (vazio(env)) env = System.getenv("ONGSAVE_" + chave.toUpperCase().replace('.', '_'));
        if (!vazio(env)) return env.trim();
        String sys = System.getProperty(chave);
        if (!vazio(sys)) return sys.trim();
        String arq = props.getProperty(chave);
        return vazio(arq) ? padrao : arq.trim();
    }

    public int inteiro(String chave, int padrao) {
        try { return Integer.parseInt(texto(chave, String.valueOf(padrao))); }
        catch (NumberFormatException e) { return padrao; }
    }

    public boolean logico(String chave, boolean padrao) {
        return Boolean.parseBoolean(texto(chave, String.valueOf(padrao)));
    }

    /* ======================= Banco de dados ======================= */

    private void montarConexao() {
        String url = System.getenv("DATABASE_URL");          // padrão do Neon / Heroku / Render
        if (vazio(url)) url = texto("db.url", "");
        usuarioBanco = texto("db.usuario", "");
        senhaBanco = texto("db.senha", "");
        if (vazio(url)) return;

        url = url.trim();
        if (url.startsWith("psql ")) url = url.substring(5).trim();          // copiou o comando "psql '...'"
        if ((url.startsWith("'") && url.endsWith("'")) || (url.startsWith("\"") && url.endsWith("\"")))
            url = url.substring(1, url.length() - 1);

        if (url.startsWith("jdbc:")) {
            jdbcUrl = url;
            return;
        }
        if (!url.startsWith("postgres://") && !url.startsWith("postgresql://"))
            throw new IllegalStateException("db.url deve começar por postgresql://, postgres:// ou jdbc:postgresql://");
        try {
            URI u = new URI(url);
            if (u.getRawUserInfo() != null) {
                String[] ui = u.getRawUserInfo().split(":", 2);
                if (vazio(usuarioBanco)) usuarioBanco = URLDecoder.decode(ui[0], StandardCharsets.UTF_8);
                if (ui.length > 1 && vazio(senhaBanco)) senhaBanco = URLDecoder.decode(ui[1], StandardCharsets.UTF_8);
            }
            String host = u.getHost();
            Map<String, String> q = new LinkedHashMap<>();
            if (u.getRawQuery() != null) {
                for (String par : u.getRawQuery().split("&")) {
                    if (par.isBlank()) continue;
                    String[] kv = par.split("=", 2);
                    q.put(kv[0], kv.length > 1 ? kv[1] : "");
                }
            }
            q.remove("channel_binding");                 // parâmetro do libpq; o driver JDBC não precisa
            q.putIfAbsent("sslmode", "require");        // o Neon exige TLS
            if (host != null && host.contains("-pooler")) q.putIfAbsent("prepareThreshold", "0"); // PgBouncer
            q.putIfAbsent("ApplicationName", "OngSave");
            q.putIfAbsent("connectTimeout", "15");      // o Neon pode levar alguns segundos a "acordar"
            q.putIfAbsent("tcpKeepAlive", "true");

            StringJoiner j = new StringJoiner("&");
            q.forEach((k, v) -> j.add(k + "=" + v));
            jdbcUrl = "jdbc:postgresql://" + host + (u.getPort() > 0 ? ":" + u.getPort() : "")
                    + (vazio(u.getRawPath()) ? "/neondb" : u.getRawPath()) + "?" + j;
        } catch (URISyntaxException e) {
            throw new IllegalStateException("db.url inválida: " + e.getMessage()
                    + ". Se a senha tiver caracteres especiais, use db.usuario e db.senha separados.", e);
        }
    }

    public boolean bancoConfigurado() { return !vazio(jdbcUrl); }
    public String getJdbcUrl() { return jdbcUrl; }
    public String getUsuarioBanco() { return usuarioBanco; }
    public String getSenhaBanco() { return senhaBanco; }

    /** URL sem credenciais, segura para aparecer nos logs. */
    public String jdbcUrlParaLog() {
        if (jdbcUrl == null) return "(não configurada)";
        return jdbcUrl.replaceAll("(?i)(password|user)=[^&]*", "$1=***");
    }

    public int poolMaximo() { return Math.max(2, inteiro("db.pool.maximo", 10)); }

    /* ======================= Regras da aplicação ======================= */

    /** Carregar dados de demonstração no primeiro arranque. */
    public boolean dadosDemo() { return logico("app.dadosDemo", true); }

    /** Exigir que o motorista esteja perto da empresa/ONG para coletar e entregar (ative em produção). */
    public boolean geoValidar() { return logico("geo.validarProximidade", false); }

    public int raioValidacaoMetros() { return inteiro("geo.raioValidacaoMetros", 150); }

    /** Geocodificar endereços com o OpenStreetMap (Nominatim) ao guardar perfis. */
    public boolean geocodificar() { return logico("geo.geocodificar", true); }

    /* ======================= Modo simulação (apresentações) ======================= */

    /** Liga o simulador: GPS e frota de motoristas automáticos. Nunca em produção. */
    public boolean simulacao() { return logico("app.simulacao", false); }

    /** Duração média de cada trecho simulado (até a empresa e até a ONG), em segundos. */
    public int simSegundosTrecho() { return inteiro("sim.segundosPorTrecho", 60); }

    /** Quantas entregas automáticas o simulador mantém em andamento ao mesmo tempo. */
    public int simFrota() { return inteiro("sim.frota", 2); }

    /** Intervalo de atualização dos painéis no modo simulação (ms). */
    public int simIntervaloMs() { return Math.max(1500, inteiro("sim.intervaloMs", 3000)); }

    /** E-mails dos motoristas conduzidos pelo apresentador (os restantes são automáticos). */
    public java.util.Set<String> simContasManuais() {
        java.util.Set<String> r = new java.util.HashSet<>();
        for (String e : texto("sim.contasManuais", "motorista@ongsave.com").split("[,;\\s]+"))
            if (!e.isBlank()) r.add(e.trim().toLowerCase());
        return r;
    }

    private static boolean vazio(String s) { return s == null || s.isBlank(); }
}
