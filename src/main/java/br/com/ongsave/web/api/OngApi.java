package br.com.ongsave.web.api;

import java.util.Map;

import br.com.ongsave.model.ErroNegocio;
import br.com.ongsave.model.Perfil;
import br.com.ongsave.service.OngService;
import jakarta.servlet.annotation.WebServlet;

/**
 * <pre>
 * GET  /api/ong/estado
 * POST /api/ong/propostas/{id}/aceitar
 * POST /api/ong/propostas/{id}/recusar      motivo
 * POST /api/ong/lotes/{id}/token            -> {codigo, em, expira, usado}
 * POST /api/ong/lotes/{id}/conferencia      kg, cond (ok|parcial|improprio), obs
 * POST /api/ong/lotes/{id}/reportar         texto
 * POST /api/ong/perfil                      campos do perfil + categorias[] + camaraFria
 * </pre>
 */
@WebServlet(name = "OngApi", urlPatterns = "/api/ong/*")
public class OngApi extends ApiBase {
    private static final long serialVersionUID = 1L;

    @Override
    protected Perfil perfil() { return Perfil.ONG; }

    @Override
    protected Object get(Pedido p) {
        if (p.e("estado")) return p.s.ong.estado(p.usuario);
        throw ErroNegocio.naoEncontrado("Recurso não encontrado.");
    }

    @Override
    protected Object post(Pedido p) {
        OngService o = p.s.ong;
        if (p.e("propostas", "*", "aceitar")) { o.aceitarProposta(p.usuario, p.id(1)); return null; }
        if (p.e("propostas", "*", "recusar")) { o.recusarProposta(p.usuario, p.id(1), p.p("motivo")); return null; }
        if (p.e("lotes", "*", "token")) return o.gerarToken(p.usuario, p.id(1));
        if (p.e("lotes", "*", "conferencia")) return o.conferencia(p.usuario, p.id(1), p.p("kg"), p.p("cond"), p.p("obs"));
        if (p.e("lotes", "*", "reportar")) { o.reportar(p.usuario, p.id(1), p.p("texto")); return null; }
        if (p.e("perfil")) {
            Map<String, Object> r = o.atualizarPerfil(p.usuario, p.campos("nome", "fantasia", "resp", "email", "tel", "familias", "capKg",
                    "horaIni", "horaFim", "alvara", "cep", "rua", "num", "compl", "bairro", "cidade", "uf"), p.lista("categorias"), p.bool("camaraFria"));
            p.atualizarNomeSessao((String) r.get("fantasia"));
            return r;
        }
        throw ErroNegocio.naoEncontrado("Recurso não encontrado.");
    }
}
