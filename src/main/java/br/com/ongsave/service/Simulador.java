package br.com.ongsave.service;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.imageio.ImageIO;

import br.com.ongsave.dao.EmpresaDAO;
import br.com.ongsave.dao.LoteDAO;
import br.com.ongsave.dao.MotoristaDAO;
import br.com.ongsave.dao.OngDAO;
import br.com.ongsave.dao.PosicaoDAO;
import br.com.ongsave.dao.UsuarioDAO;
import br.com.ongsave.db.Banco;
import br.com.ongsave.db.Sql;
import br.com.ongsave.model.Conta;
import br.com.ongsave.model.ErroNegocio;
import br.com.ongsave.model.Posicao;
import br.com.ongsave.model.Perfil;
import br.com.ongsave.model.Usuario;
import br.com.ongsave.util.Geo;

/**
 * Modo simulação (app.simulacao=true), pensado para apresentações.
 *
 * <p>O simulador faz o papel do GPS e de uma pequena frota de motoristas, sempre através dos serviços reais
 * (as mesmas regras, notificações e gravações no banco de um uso verdadeiro):</p>
 * <ul>
 *   <li><b>Motoristas automáticos</b> (todos os ativos, menos os de {@code sim.contasManuais}): aceitam os lotes
 *       gerados pela simulação, vão até a empresa pelas ruas reais, recolhem com o código de retirada, seguem até
 *       a ONG, validam com token + foto e a ONG confere o recebimento.</li>
 *   <li><b>Motorista do apresentador</b> ({@code sim.contasManuais}, por padrão motorista@ongsave.com): só anda quando
 *       se carrega em "Simular trajeto" na Rota Ativa; código, token e foto são feitos à mão, como na vida real.</li>
 *   <li><b>Lotes automáticos</b>: mantém {@code sim.frota} entregas em andamento, publicadas pelas empresas ativas.
 *       Lotes publicados à mão nunca são tocados pelos motoristas automáticos.</li>
 * </ul>
 * Com app.simulacao=false nada disto arranca e a aplicação usa só o GPS real dos celulares.
 */
public class Simulador {

    private static final Logger LOG = Logger.getLogger(Simulador.class.getName());
    private static final long TICK_MS = 2000;
    private static final Pattern PAR = Pattern.compile("\\[\\s*(-?\\d+(?:\\.\\d+)?)\\s*,\\s*(-?\\d+(?:\\.\\d+)?)\\s*]");

    private final EmpresaService empresa;
    private final MotoristaService motorista;
    private final OngService ong;
    private final int segundosTrecho;
    private final int frota;
    private final Set<String> contasManuais;
    private final LoteDAO lotes = new LoteDAO();
    private final PosicaoDAO posicoes = new PosicaoDAO();
    private final UsuarioDAO usuarios = new UsuarioDAO();
    private final MotoristaDAO motoristas = new MotoristaDAO();
    private final EmpresaDAO empresasDao = new EmpresaDAO();
    private final OngDAO ongsDao = new OngDAO();

    /** Trecho em curso por lote (motorista a caminho da empresa ou da ONG). */
    private final Map<Long, Trecho> trechos = new ConcurrentHashMap<>();
    /** Lotes do apresentador para os quais foi pedido "Simular trajeto". */
    private final Set<Long> pedidos = ConcurrentHashMap.newKeySet();
    /** Rotas já calculadas (OSRM), por par de coordenadas. */
    private final Map<String, List<double[]>> rotas = new ConcurrentHashMap<>();
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(4)).build();

    private ScheduledExecutorService agenda;
    private volatile long ultimoLoteMs = 0;
    private volatile long ultimoErroMs = 0;

    public Simulador(EmpresaService empresa, MotoristaService motorista, OngService ong,
                     int segundosTrecho, int frota, Set<String> contasManuais) {
        this.empresa = empresa;
        this.motorista = motorista;
        this.ong = ong;
        this.segundosTrecho = Math.max(15, segundosTrecho);
        this.frota = Math.max(0, frota);
        this.contasManuais = contasManuais;
    }

    /* ============================ Ciclo de vida ============================ */

    public synchronized void iniciar() {
        if (agenda != null) return;
        if (System.getProperty("java.awt.headless") == null) System.setProperty("java.awt.headless", "true");
        agenda = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "ongsave-simulador");
            t.setDaemon(true);
            return t;
        });
        agenda.scheduleWithFixedDelay(this::tick, 3000, TICK_MS, TimeUnit.MILLISECONDS);
        LOG.info(() -> "OngSave: MODO SIMULAÇÃO ligado (frota automática: " + frota + ", ~" + segundosTrecho
                + " s por trecho). Desligue app.simulacao em produção.");
    }

    public synchronized void parar() {
        if (agenda != null) agenda.shutdownNow();
        agenda = null;
    }

    /** Pedido do apresentador: o motorista começa a andar até ao destino da fase atual. */
    public Map<String, Object> conduzir(Usuario u) {
        Map<String, Object> l = Banco.ler(c -> {
            return lotes.detalheAtivoDoMotorista(c, u.getId());
        });
        if (l == null) throw ErroNegocio.naoEncontrado("Você não tem entrega em andamento.");
        long id = Sql.l(l, "id");
        String alvo = "aceito".equals(l.get("estado")) ? "empresa" : "ong";
        Trecho t = trechos.get(id);
        if (t != null && t.alvo.equals(alvo) && !t.chegou()) return Map.of("estado", "andando", "alvo", alvo);
        if (t != null && t.alvo.equals(alvo) && t.chegou()) return Map.of("estado", "chegou", "alvo", alvo);
        pedidos.add(id);
        return Map.of("estado", "iniciado", "alvo", alvo);
    }

    /* ============================ Ciclo principal ============================ */

    private void tick() {
        try {
            Set<Long> manuais = idsManuais();
            avancarTrechos(manuais);
            conferirRecebidos(manuais);
            aceitarComoOng();
            aceitarComoMotorista(manuais);
            publicarLotes();
        } catch (RuntimeException e) {
            long agora = System.currentTimeMillis();
            if (agora - ultimoErroMs > 60_000) {           // não inunda a consola se o banco cair
                ultimoErroMs = agora;
                LOG.log(Level.WARNING, "OngSave: falha no ciclo do simulador", e);
            }
        }
    }

    private Set<Long> idsManuais() {
        return Banco.ler(c -> {
            Set<Long> r = new HashSet<>();
            for (Conta m : usuarios.listarPorPerfil(c, Perfil.MOTORISTA))
                if (m.getEmail() != null && contasManuais.contains(m.getEmail().toLowerCase(Locale.ROOT))) r.add(m.getId());
            return r;
        });
    }

    /** Move cada motorista ao longo da rota e grava a posição, como faria o GPS do celular. */
    private void avancarTrechos(Set<Long> manuais) {
        List<Map<String, Object>> ativos = Banco.ler(lotes::detalhesEmAndamento);
        Set<Long> vivos = new HashSet<>();
        for (Map<String, Object> l : ativos) {
            long id = Sql.l(l, "id");
            vivos.add(id);
            boolean automatico = !manuais.contains(Sql.l(l, "motoristaId"));
            String alvo = "aceito".equals(l.get("estado")) ? "empresa" : "ong";

            Trecho t = trechos.get(id);
            if (t != null && !t.alvo.equals(alvo)) { trechos.remove(id); t = null; }
            if (t == null) {
                if (!automatico && !pedidos.remove(id)) continue;          // o apresentador ainda não pediu
                t = novoTrecho(l, alvo);
                trechos.put(id, t);
            }
            if (t.terminado) continue;

            double[] p = t.posicao(System.currentTimeMillis());
            gravarPosicao(id, Sql.l(l, "motoristaId"), p);
            if (t.chegou()) {
                t.terminado = true;
                if (automatico) agirNaChegada(l, alvo, p);
            }
        }
        trechos.keySet().removeIf(id -> !vivos.contains(id));
        pedidos.removeIf(id -> !vivos.contains(id));
    }

    private Trecho novoTrecho(Map<String, Object> l, String alvo) {
        double[] destino = "empresa".equals(alvo)
                ? new double[] { Sql.d(l, "empLat"), Sql.d(l, "empLon") }
                : new double[] { Sql.d(l, "ongLat"), Sql.d(l, "ongLon") };
        double[] origem;
        if (l.get("posLat") != null)
            origem = new double[] { Sql.d(l, "posLat"), Sql.d(l, "posLon") };
        else if ("ong".equals(alvo))
            origem = new double[] { Sql.d(l, "empLat"), Sql.d(l, "empLon") };
        else
            origem = pontoProximo(destino, 1800 + ThreadLocalRandom.current().nextInt(1500));
        List<double[]> pts = rota(origem, destino);
        double km = 0;
        for (int i = 1; i < pts.size(); i++) km += Geo.km(pts.get(i - 1)[0], pts.get(i - 1)[1], pts.get(i)[0], pts.get(i)[1]);
        double fator = Math.max(0.6, Math.min(1.8, km / 4.0));        // trechos maiores demoram um pouco mais
        return new Trecho(alvo, pts, System.currentTimeMillis(), (long) (segundosTrecho * fator * 1000));
    }

    private void gravarPosicao(long loteId, long motoristaId, double[] p) {
        Posicao pos = new Posicao();
        pos.setLoteId(loteId);
        pos.setMotoristaId(motoristaId);
        pos.setLat(p[0]);
        pos.setLon(p[1]);
        pos.setPrecisaoM(5f);
        Banco.transacao(c -> { posicoes.inserir(c, pos); return null; });
    }

    /** Motorista automático chegou: faz o que empresa, motorista e ONG fariam no local. */
    private void agirNaChegada(Map<String, Object> l, String alvo, double[] p) {
        long id = Sql.l(l, "id");
        Usuario mot = new Usuario(Sql.l(l, "motoristaId"), Sql.s(l, "mot"), "", Perfil.MOTORISTA);
        try {
            if ("empresa".equals(alvo)) {
                String codigo = Sql.s(l, "retCodigo");
                if (codigo == null) {
                    Usuario emp = new Usuario(Sql.l(l, "empresaId"), Sql.s(l, "emp"), "", Perfil.EMPRESA);
                    String temp = "amb".equals(l.get("cons")) ? "" : ("cong".equals(l.get("cons")) ? "-18" : "4");
                    Map<String, Object> r = empresa.registrarRetirada(emp, id, numero(l.get("kg")), temp, "Conferido no balcão.");
                    codigo = (String) r.get("codigo");
                }
                motorista.coletar(mot, p[0], p[1], codigo);
            } else {
                Usuario org = new Usuario(Sql.l(l, "ongId"), Sql.s(l, "ong"), "", Perfil.ONG);
                Map<String, Object> tok = ong.gerarToken(org, id);
                motorista.finalizar(mot, (String) tok.get("codigo"), fotoDescarga(id, Sql.s(l, "ong")), p[0], p[1],
                        DateTimeFormatter.ISO_INSTANT.format(Instant.now().truncatedTo(ChronoUnit.SECONDS)));
            }
        } catch (RuntimeException e) {
            LOG.log(Level.FINE, "Simulador: ação na chegada do lote #" + id + " falhou", e);
            trechos.remove(id);                                       // tenta de novo no próximo ciclo
        }
    }

    /** A ONG confere os lotes automáticos ~10 s depois da entrega (peso igual, boa condição). */
    private void conferirRecebidos(Set<Long> manuais) {
        List<Map<String, Object>> ls = Banco.ler(lotes::simuladosParaConferir);
        for (Map<String, Object> l : ls) {
            if (manuais.contains(Sql.l(l, "motorista_id"))) continue;    // o apresentador confere à mão
            Usuario org = new Usuario(Sql.l(l, "ong_id"), Sql.s(l, "nome"), "", Perfil.ONG);
            try { ong.conferencia(org, Sql.l(l, "id"), numero(l.get("kg")), "ok", "Recebido em boas condições."); }
            catch (RuntimeException e) { LOG.log(Level.FINE, "Simulador: conferência falhou", e); }
        }
    }

    /** A ONG aceita as propostas automáticas ~6 s depois de publicadas. */
    private void aceitarComoOng() {
        List<Map<String, Object>> ls = Banco.ler(lotes::simuladosAguardandoOng);
        for (Map<String, Object> l : ls) {
            long id = Sql.l(l, "id");
            try {
                ong.aceitarProposta(new Usuario(Sql.l(l, "ong_id"), Sql.s(l, "nome"), "", Perfil.ONG), id);
            } catch (RuntimeException e) {
                Banco.transacao(c -> { lotes.cancelarSeAguardando(c, id); return null; });
            }
        }
    }

    /** Um motorista automático livre e com veículo compatível aceita cada lote automático já aceite pela ONG. */
    private void aceitarComoMotorista(Set<Long> manuais) {
        List<Map<String, Object>> pendentes = Banco.ler(lotes::simuladosAguardandoMotorista);
        if (pendentes.isEmpty()) return;
        List<Map<String, Object>> livres = Banco.ler(motoristas::listarLivres);
        livres.removeIf(m -> manuais.contains(Sql.l(m, "id")));
        for (Map<String, Object> l : pendentes) {
            Map<String, Object> escolhido = null;
            for (Map<String, Object> m : livres)
                if (Lotes.rankMotorista(Sql.s(m, "veiculo")) >= Lotes.rankLote(Sql.s(l, "veiculo"))) { escolhido = m; break; }
            if (escolhido == null) continue;
            livres.remove(escolhido);
            long motId = Sql.l(escolhido, "id");
            Banco.transacao(c -> { motoristas.concederConsentimentoSeFaltar(c, motId); return null; });
            try { motorista.aceitar(new Usuario(motId, Sql.s(escolhido, "nome"), "", Perfil.MOTORISTA), Sql.l(l, "id")); }
            catch (RuntimeException e) { LOG.log(Level.FINE, "Simulador: aceite do motorista falhou", e); }
        }
    }

    /** Mantém a frota ocupada: publica um lote novo quando há menos de sim.frota lotes automáticos em curso. */
    private void publicarLotes() {
        if (frota == 0 || System.currentTimeMillis() - ultimoLoteMs < 35_000) return;
        long emCurso = Banco.ler(lotes::contarSimuladosEmCurso);
        if (emCurso >= frota) return;
        ultimoLoteMs = System.currentTimeMillis();

        ThreadLocalRandom rnd = ThreadLocalRandom.current();
        List<String> cats = EmpresaService.CATEGORIAS;
        String cat = cats.get(rnd.nextInt(cats.size()));
        String cons = cat.startsWith("Latic") ? "ref" : "amb";
        int kg = 12 + rnd.nextInt(55);
        String veic = kg > 45 && rnd.nextBoolean() ? "van" : "carro";

        boolean preferirEmp = rnd.nextInt(100) < 50, preferirOng = rnd.nextInt(100) < 50;
        Map<String, Object> par = Banco.ler(c -> escolherPar(c, cat, cons, kg, preferirEmp, preferirOng));
        if (par == null) return;
        Usuario emp = new Usuario(Sql.l(par, "empId"), Sql.s(par, "empNome"), "", Perfil.EMPRESA);
        String validade = Instant.now().plus(Duration.ofHours(6 + rnd.nextInt(30))).truncatedTo(ChronoUnit.SECONDS).toString();
        String[] obs = { "Retirar na doca de carga.", "Falar com o gerente de turno.", "Caixas identificadas com etiqueta verde.", "" };
        try {
            Map<String, Object> r = empresa.publicar(emp, new EmpresaService.NovoLote(cat, String.valueOf(kg), String.valueOf(1 + kg / 10),
                    cons, validade, null, null, veic, String.valueOf(Sql.l(par, "ongId")), obs[rnd.nextInt(obs.length)], true,
                    fotoLote(cat, kg)));
            long id = ((Number) r.get("id")).longValue();
            Banco.transacao(c -> { lotes.marcarSimulado(c, id); return null; });
        } catch (RuntimeException e) {
            LOG.log(Level.FINE, "Simulador: publicação de lote falhou", e);
        }
    }

    /**
     * Empresa ativa e ONG compatível com capacidade livre hoje (a mais próxima entre 3 sorteadas).
     * Em metade das vezes prefere as contas de demonstração (empresa@ / ong@ongsave.com), para que os painéis
     * usados na apresentação tenham sempre movimento.
     */
    private Map<String, Object> escolherPar(Connection c, String cat, String cons, int kg,
                                                   boolean preferirEmp, boolean preferirOng) throws SQLException {
        Map<String, Object> e = empresasDao.sortearAtiva(c, preferirEmp);
        if (e == null) return null;
        List<Map<String, Object>> ongs = ongsDao.sortearComCapacidade(c, cat, cons, kg, preferirOng);
        if (ongs.isEmpty()) return null;
        if (preferirOng && Boolean.TRUE.equals(ongs.get(0).get("demo"))
                && Geo.metros(Sql.d(e, "lat"), Sql.d(e, "lon"), Sql.d(ongs.get(0), "lat"), Sql.d(ongs.get(0), "lon")) > 300) {
            e.put("ongId", ongs.get(0).get("id"));
            return e;
        }
        Map<String, Object> melhor = null;
        double dist = Double.MAX_VALUE;
        for (Map<String, Object> o : ongs) {
            double d = Geo.metros(Sql.d(e, "lat"), Sql.d(e, "lon"), Sql.d(o, "lat"), Sql.d(o, "lon"));
            if (d < dist && d > 300) { dist = d; melhor = o; }
        }
        if (melhor == null) return null;
        e.put("ongId", melhor.get("id"));
        return e;
    }

    /* ============================ Rotas ============================ */

    /** Rota pelas ruas (OSRM / OpenStreetMap). Sem internet: linha reta com pontos intermédios. */
    private List<double[]> rota(double[] de, double[] para) {
        String chave = String.format(Locale.ROOT, "%.4f,%.4f;%.4f,%.4f", de[0], de[1], para[0], para[1]);
        List<double[]> pronta = rotas.get(chave);
        if (pronta != null) return pronta;
        List<double[]> pts = new ArrayList<>();
        try {
            String url = String.format(Locale.ROOT, "https://router.project-osrm.org/route/v1/driving/%.6f,%.6f;%.6f,%.6f?overview=full&geometries=geojson",
                    de[1], de[0], para[1], para[0]);
            HttpResponse<String> r = http.send(HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(6))
                    .header("User-Agent", "OngSave/1.0 (simulacao)").GET().build(), HttpResponse.BodyHandlers.ofString());
            String corpo = r.body();
            int i = corpo.indexOf("\"coordinates\"");
            if (r.statusCode() == 200 && i >= 0) {
                String trecho = corpo.substring(i, corpo.indexOf("]]", i) + 2);
                Matcher m = PAR.matcher(trecho);
                while (m.find()) pts.add(new double[] { Double.parseDouble(m.group(2)), Double.parseDouble(m.group(1)) });
            }
        } catch (Exception e) {
            LOG.log(Level.FINE, "Simulador: OSRM indisponível, a usar linha reta", e);
        }
        if (pts.size() < 2) {
            pts.clear();
            for (int k = 0; k <= 20; k++) {
                double f = k / 20.0;
                pts.add(new double[] { de[0] + (para[0] - de[0]) * f, de[1] + (para[1] - de[1]) * f });
            }
        } else {
            pts.add(0, de.clone());
            pts.add(para.clone());
        }
        if (rotas.size() > 500) rotas.clear();
        rotas.put(chave, pts);
        return pts;
    }

    private static double[] pontoProximo(double[] centro, double metros) {
        double ang = ThreadLocalRandom.current().nextDouble(Math.PI * 2);
        double dLat = metros * Math.cos(ang) / 111_320.0;
        double dLon = metros * Math.sin(ang) / (111_320.0 * Math.cos(Math.toRadians(centro[0])));
        return new double[] { centro[0] + dLat, centro[1] + dLon };
    }

    /** Percurso com tempo: a posição em cada instante é interpolada pela distância percorrida. */
    private static final class Trecho {
        final String alvo;
        final List<double[]> pts;
        final double[] acum;
        final long inicio, duracao;
        volatile boolean terminado;

        Trecho(String alvo, List<double[]> pts, long inicio, long duracao) {
            this.alvo = alvo;
            this.pts = pts;
            this.inicio = inicio;
            this.duracao = Math.max(5000, duracao);
            acum = new double[pts.size()];
            for (int i = 1; i < pts.size(); i++)
                acum[i] = acum[i - 1] + Geo.metros(pts.get(i - 1)[0], pts.get(i - 1)[1], pts.get(i)[0], pts.get(i)[1]);
        }

        boolean chegou() { return System.currentTimeMillis() >= inicio + duracao; }

        double[] posicao(long agora) {
            double f = Math.max(0, Math.min(1, (agora - inicio) / (double) duracao));
            f = f * f * (3 - 2 * f);                                   // arranca e trava suavemente
            double alvoM = acum[acum.length - 1] * f;
            for (int i = 1; i < pts.size(); i++) {
                if (acum[i] >= alvoM) {
                    double seg = acum[i] - acum[i - 1], t = seg <= 0 ? 1 : (alvoM - acum[i - 1]) / seg;
                    double[] a = pts.get(i - 1), b = pts.get(i);
                    return new double[] { a[0] + (b[0] - a[0]) * t, a[1] + (b[1] - a[1]) * t };
                }
            }
            return pts.get(pts.size() - 1).clone();
        }
    }

    /* ============================ Imagens ============================ */

    private static String numero(Object kg) {
        return kg == null ? "1" : String.format(Locale.ROOT, "%.1f", ((Number) kg).doubleValue());
    }

    private static byte[] fotoLote(String categoria, int kg) {
        return desenhar(new Color(234, 88, 12), new Color(251, 146, 60), "Lote de doação", categoria, kg + " kg");
    }

    private static byte[] fotoDescarga(long id, String ong) {
        return desenhar(new Color(5, 150, 105), new Color(52, 211, 153), "Descarga · Lote #" + id, ong,
                DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm").withZone(Estatisticas.FUSO).format(Instant.now()));
    }

    /** Imagem PNG gerada no servidor (substitui a câmera nos passos automáticos). */
    private static byte[] desenhar(Color c1, Color c2, String titulo, String linha1, String linha2) {
        try {
            BufferedImage img = new BufferedImage(640, 420, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = img.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g.setPaint(new GradientPaint(0, 0, c1, 640, 420, c2));
            g.fillRect(0, 0, 640, 420);
            g.setColor(new Color(255, 255, 255, 60));
            for (int i = 0; i < 6; i++) g.fillRoundRect(60 + i * 90, 210 - (i % 3) * 30, 80, 120 + (i % 3) * 30, 10, 10);
            g.setColor(new Color(255, 255, 255, 120));
            g.setStroke(new BasicStroke(3));
            for (int i = 0; i < 6; i++) g.drawLine(100 + i * 90, 210 - (i % 3) * 30, 100 + i * 90, 330);
            g.setColor(Color.WHITE);
            g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 34));
            g.drawString(titulo, 40, 70);
            g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 24));
            g.drawString(linha1 == null ? "" : linha1, 40, 110);
            g.drawString(linha2 == null ? "" : linha2, 40, 145);
            g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 18));
            g.drawString("OngSave · imagem de demonstração", 40, 390);
            g.dispose();
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ImageIO.write(img, "png", out);
            return out.toByteArray();
        } catch (Exception | Error e) {                               // ambiente sem suporte gráfico: PNG mínimo
            return Base64.getDecoder().decode("iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNk+M9QDwADhgGAWjR9awAAAABJRU5ErkJggg==");
        }
    }
}
