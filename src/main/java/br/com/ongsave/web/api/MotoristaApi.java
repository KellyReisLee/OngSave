package br.com.ongsave.web.api;

import java.io.IOException;
import java.util.Map;

import br.com.ongsave.model.ErroNegocio;
import br.com.ongsave.model.Perfil;
import br.com.ongsave.service.MotoristaService;
import br.com.ongsave.util.Json;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;

/**
 * <pre>
 * GET  /api/motorista/estado
 * GET  /api/motorista/ofertas?lat&amp;lon
 * GET  /api/motorista/entrega             -> {entrega: {... fase: coleta|transito} | null}
 * POST /api/motorista/aceitar             id            (409 se outro motorista aceitou primeiro)
 * POST /api/motorista/coleta              lat, lon, codigo
 * POST /api/motorista/posicao             lat, lon, accuracy
 * POST /api/motorista/finalizar           multipart: token, foto, lat, lon, capturada_em
 * POST /api/motorista/perfil | consentimento | sacar | contestar
 * </pre>
 */
@WebServlet(name = "MotoristaApi", urlPatterns = "/api/motorista/*")
@MultipartConfig(maxFileSize = 9 * 1024 * 1024, maxRequestSize = 10 * 1024 * 1024, fileSizeThreshold = 512 * 1024)
public class MotoristaApi extends ApiBase {
    private static final long serialVersionUID = 1L;

    @Override
    protected Perfil perfil() { return Perfil.MOTORISTA; }

    @Override
    protected Object get(Pedido p) {
        MotoristaService m = p.s.motorista;
        if (p.e("estado")) return m.estado(p.usuario);
        if (p.e("ofertas")) return m.ofertas(p.usuario, p.num("lat"), p.num("lon"));
        if (p.e("entrega")) return Json.obj("entrega", m.entregaAtiva(p.usuario));
        throw ErroNegocio.naoEncontrado("Recurso não encontrado.");
    }

    @Override
    protected Object post(Pedido p) throws IOException, ServletException {
        MotoristaService m = p.s.motorista;
        switch (p.rota.isEmpty() ? "" : String.join("/", p.rota)) {
            case "aceitar" -> {
                long id;
                try { id = Long.parseLong(p.p("id")); } catch (RuntimeException e) { throw ErroNegocio.invalido("Entrega inválida."); }
                return m.aceitar(p.usuario, id);
            }
            case "coleta" -> { m.coletar(p.usuario, p.num("lat"), p.num("lon"), p.p("codigo")); return null; }
            case "posicao" -> {
                double acc = p.num("accuracy");
                m.registrarPosicao(p.usuario, p.num("lat"), p.num("lon"), Double.isNaN(acc) ? null : acc);
                return null;
            }
            case "finalizar" -> {
                m.finalizar(p.usuario, p.p("token"), p.arquivo("foto", 8 * 1024 * 1024), p.num("lat"), p.num("lon"), p.p("capturada_em"));
                return null;
            }
            case "perfil" -> {
                Map<String, Object> r = m.atualizarPerfil(p.usuario, p.campos("nome", "email", "tel", "veiculo", "placa", "modelo", "cnh", "cat",
                        "cnhValidade", "cep", "rua", "num", "bairro", "cidade", "uf"));
                p.atualizarNomeSessao((String) r.get("nome"));
                return r;
            }
            case "consentimento" -> { return m.consentimento(p.usuario, p.bool("conceder")); }
            case "sacar" -> { return m.sacar(p.usuario, p.p("valor")); }
            case "simular" -> {
                if (p.s.simulador == null) throw ErroNegocio.naoEncontrado("O modo simulação está desligado.");
                return p.s.simulador.conduzir(p.usuario);
            }
            case "contestar" -> { m.contestar(p.usuario, p.p("texto")); return null; }
            default -> throw ErroNegocio.naoEncontrado("Recurso não encontrado.");
        }
    }
}
