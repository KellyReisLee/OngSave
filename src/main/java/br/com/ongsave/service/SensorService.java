package br.com.ongsave.service;

import java.util.List;
import java.util.Locale;
import java.util.Map;

import br.com.ongsave.dao.SensorDAO;
import br.com.ongsave.db.Banco;
import br.com.ongsave.db.Sql;
import br.com.ongsave.model.ErroNegocio;
import br.com.ongsave.util.Json;

/**
 * Leituras dos sensores IoT (simulados pelo script iot/simulador_sensores.py).
 *
 * <ul>
 *   <li><b>Temperatura</b> (câmara fria da ONG): alerta quando a leitura sai da faixa segura do sensor (2 a 8 °C).
 *       Avisa a ONG, porque um lote refrigerado não deve ser recebido numa câmara quente.</li>
 *   <li><b>Peso</b> (balança da doca da empresa): ao pesar um lote, compara com o peso declarado na publicação.
 *       Se a diferença passar da tolerância da plataforma (regras.tolPesoPct, 5 % por padrão), avisa a empresa
 *       e a ONG de destino, antes de o motorista sair com a carga.</li>
 * </ul>
 * Na temperatura, para não inundar as notificações, só avisa quando o sensor <i>entra</i> em alerta (a leitura anterior
 * estava normal). Na balança, cada pesagem fora da tolerância é avisada.
 */
public class SensorService {

    private final SensorDAO sensores = new SensorDAO();
    private final PlataformaService plataforma;
    private final NotificacaoService notificacoes;
    private final AuditoriaService auditoria;

    public SensorService(PlataformaService plataforma, NotificacaoService notificacoes, AuditoriaService auditoria) {
        this.plataforma = plataforma;
        this.notificacoes = notificacoes;
        this.auditoria = auditoria;
    }

    public List<Map<String, Object>> listar() {
        return Banco.ler(sensores::listar);
    }

    public List<Map<String, Object>> leituras(String sensorId, int limite) {
        return Banco.ler(c -> {
            if (sensores.buscar(c, sensorId) == null) throw ErroNegocio.naoEncontrado("Sensor não encontrado.");
            return sensores.leituras(c, sensorId, Math.max(1, Math.min(limite, 200)));
        });
    }

    /** Grava uma leitura e devolve {id, sensor, valor, alerta, motivo}. */
    public Map<String, Object> registrar(String sensorId, Object valorBruto, Object loteBruto) {
        if (sensorId == null || sensorId.isBlank()) throw ErroNegocio.invalido("Informe o sensor.");
        if (!(valorBruto instanceof Number n) || !Double.isFinite(n.doubleValue()))
            throw ErroNegocio.invalido("Informe o valor da leitura (número).");
        double valor = n.doubleValue();
        Long loteId = null;
        if (loteBruto != null) {
            if (!(loteBruto instanceof Number ln)) throw ErroNegocio.invalido("Lote inválido.");
            loteId = ln.longValue();
        }
        final Long lote = loteId;
        double tol = plataforma.tolPesoPct();

        return Banco.transacao(c -> {
            Map<String, Object> s = sensores.buscar(c, sensorId.trim());
            if (s == null || !Boolean.TRUE.equals(s.get("ativo"))) throw ErroNegocio.naoEncontrado("Sensor não encontrado.");
            String id = Sql.s(s, "id");
            String tipo = Sql.s(s, "tipo");
            long dono = Sql.l(s, "donoId");
            Double min = s.get("limiteMin") == null ? null : Sql.d(s, "limiteMin");
            Double max = s.get("limiteMax") == null ? null : Sql.d(s, "limiteMax");

            boolean alerta = false;
            String motivo = null;
            Long avisarTambem = null;                              // ONG de destino, nos alertas de peso

            if ("temperatura".equals(tipo)) {
                if (valor < -40 || valor > 80) throw ErroNegocio.invalido("Temperatura fora do alcance do sensor (-40 a 80 °C).");
                if (lote != null) throw ErroNegocio.invalido("O sensor de temperatura não pesa lotes.");
                if ((min != null && valor < min) || (max != null && valor > max)) {
                    alerta = true;
                    motivo = String.format(Locale.ROOT, "%s a %s °C, fora da faixa segura (%s a %s °C).",
                            Sql.s(s, "descricao"), EmpresaService.fmt(valor), EmpresaService.fmt(min), EmpresaService.fmt(max));
                }
            } else {
                if (valor < 0) throw ErroNegocio.invalido("O peso não pode ser negativo.");
                if (max != null && valor > max) {
                    alerta = true;
                    motivo = String.format(Locale.ROOT, "%s: %s kg passa da capacidade da balança (%s kg).",
                            Sql.s(s, "descricao"), EmpresaService.fmt(valor), EmpresaService.fmt(max));
                } else if (lote != null) {
                    Map<String, Object> l = sensores.loteParaPesagem(c, lote, dono);
                    if (l == null) throw ErroNegocio.naoEncontrado("Lote não encontrado ou já saiu da doca.");
                    double declarado = Sql.d(l, "kg");
                    double div = declarado == 0 ? 0 : (valor - declarado) / declarado * 100;
                    if (Math.abs(div) > tol) {
                        alerta = true;
                        motivo = String.format(Locale.ROOT, "Lote #%d pesou %s kg na balança, mas foi declarado com %s kg (%s%s %%, tolerância %s %%).",
                                lote, EmpresaService.fmt(valor), EmpresaService.fmt(declarado), div > 0 ? "+" : "",
                                EmpresaService.fmt(Math.round(div * 10) / 10.0), EmpresaService.fmt(tol));
                        avisarTambem = l.get("ongId") == null ? null : Sql.l(l, "ongId");
                    }
                }
            }

            boolean avisar = alerta && ("peso".equals(tipo) || !sensores.ultimaEmAlerta(c, id));
            long leitura = sensores.inserirLeitura(c, id, lote, valor, alerta, motivo);
            if (avisar) {
                String icone = "temperatura".equals(tipo) ? "fa-temperature-high" : "fa-weight-scale";
                notificacoes.notificar(c, dono, "Alerta do sensor: " + motivo, icone);
                if (avisarTambem != null) notificacoes.notificar(c, avisarTambem, "Alerta do sensor: " + motivo, icone);
                auditoria.registrar(c, null, "Alerta de sensor IoT", id, lote, motivo);
            }
            return Json.obj("id", leitura, "sensor", id, "tipo", tipo, "valor", valor, "lote", lote,
                    "alerta", alerta, "motivo", motivo, "notificado", avisar);
        });
    }
}
