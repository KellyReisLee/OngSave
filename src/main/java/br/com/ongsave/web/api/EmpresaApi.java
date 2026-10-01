package br.com.ongsave.web.api;

import java.io.IOException;
import java.util.Map;

import br.com.ongsave.model.ErroNegocio;
import br.com.ongsave.model.Perfil;
import br.com.ongsave.service.EmpresaService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;

/**
 * <pre>
 * GET  /api/empresa/estado
 * POST /api/empresa/lotes                    multipart: tipo, kg, vol, cons, validade, j1, j2, veic, ong, obs, termo, foto
 * POST /api/empresa/lotes/{id}/cancelar
 * POST /api/empresa/lotes/{id}/retirada      kg, temp, obs   -> {codigo, ...}
 * POST /api/empresa/perfil                   nome, email, tel, descarte, cep, rua, num, compl, bairro, cidade, uf
 * POST /api/empresa/plano                    plano
 * </pre>
 */
@WebServlet(name = "EmpresaApi", urlPatterns = "/api/empresa/*")
@MultipartConfig(maxFileSize = 6 * 1024 * 1024, maxRequestSize = 7 * 1024 * 1024, fileSizeThreshold = 512 * 1024)
public class EmpresaApi extends ApiBase {
    private static final long serialVersionUID = 1L;

    @Override
    protected Perfil perfil() { return Perfil.EMPRESA; }

    @Override
    protected Object get(Pedido p) {
        if (p.e("estado")) return p.s.empresa.estado(p.usuario);
        throw ErroNegocio.naoEncontrado("Recurso não encontrado.");
    }

    @Override
    protected Object post(Pedido p) throws IOException, ServletException {
        EmpresaService e = p.s.empresa;
        if (p.e("lotes")) {
            return e.publicar(p.usuario, new EmpresaService.NovoLote(p.p("tipo"), p.p("kg"), p.p("vol"), p.p("cons"), p.p("validade"),
                    p.p("j1"), p.p("j2"), p.p("veic"), p.p("ong"), p.p("obs"), p.bool("termo"), p.arquivo("foto", 5 * 1024 * 1024)));
        }
        if (p.e("lotes", "*", "cancelar")) { e.cancelar(p.usuario, p.id(1)); return null; }
        if (p.e("lotes", "*", "retirada")) return e.registrarRetirada(p.usuario, p.id(1), p.p("kg"), p.p("temp"), p.p("obs"));
        if (p.e("perfil")) {
            Map<String, Object> r = e.atualizarPerfil(p.usuario, p.campos("nome", "email", "tel", "descarte", "cep", "rua", "num", "compl", "bairro", "cidade", "uf"));
            p.atualizarNomeSessao((String) r.get("nome"));
            return r;
        }
        if (p.e("plano")) return e.trocarPlano(p.usuario, p.p("plano"));
        throw ErroNegocio.naoEncontrado("Recurso não encontrado.");
    }
}
