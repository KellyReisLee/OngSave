package br.com.ongsave.web.api;

import java.util.Map;

import br.com.ongsave.model.ErroNegocio;
import br.com.ongsave.model.Perfil;
import br.com.ongsave.service.AdminService;
import jakarta.servlet.annotation.WebServlet;

/**
 * <pre>
 * GET  /api/admin/estado
 * GET  /api/admin/documentos/{id}/arquivo
 * POST /api/admin/usuarios/{id}/status        status, motivo, obs
 * POST /api/admin/usuarios/{id}/notas         texto
 * POST /api/admin/usuarios/{id}/solicitar     documento, obs
 * POST /api/admin/documentos/{id}             status (ok|rejeitado), motivo, obs
 * POST /api/admin/ocorrencias/{id}/analisar
 * POST /api/admin/ocorrencias/{id}/resolver   motivo, obs
 * POST /api/admin/plataforma                  JSON com os parâmetros (raioKm, fretes, planos, regras, metodologia...)
 * </pre>
 */
@WebServlet(name = "AdminApi", urlPatterns = "/api/admin/*")
public class AdminApi extends ApiBase {
    private static final long serialVersionUID = 1L;

    @Override
    protected Perfil perfil() { return Perfil.ADMIN; }

    @Override
    protected Object get(Pedido p) {
        if (p.e("estado")) return p.s.admin.estado(p.usuario);
        if (p.e("documentos", "*", "arquivo")) {
            Map<String, Object> d = p.s.admin.arquivoDocumento(p.id(1));
            return new Arquivo((byte[]) d.get("bytes"), (String) d.get("content_type"), (String) d.get("arquivo_nome"));
        }
        throw ErroNegocio.naoEncontrado("Recurso não encontrado.");
    }

    @Override
    protected Object post(Pedido p) {
        AdminService a = p.s.admin;
        if (p.e("usuarios", "*", "status")) return a.mudarStatus(p.usuario, p.id(1), p.p("status"), p.p("motivo"), p.p("obs"));
        if (p.e("usuarios", "*", "notas")) return a.nota(p.usuario, p.id(1), p.p("texto"));
        if (p.e("usuarios", "*", "solicitar")) { a.solicitarDocumento(p.usuario, p.id(1), p.p("documento"), p.p("obs")); return null; }
        if (p.e("documentos", "*")) return a.documento(p.usuario, p.id(1), p.p("status"), p.p("motivo"), p.p("obs"));
        if (p.e("ocorrencias", "*", "analisar")) { a.analisarOcorrencia(p.usuario, p.id(1)); return null; }
        if (p.e("ocorrencias", "*", "resolver")) { a.resolverOcorrencia(p.usuario, p.id(1), p.p("motivo"), p.p("obs")); return null; }
        if (p.e("plataforma")) return p.s.plataforma.atualizar(p.corpo(), p.usuario);
        throw ErroNegocio.naoEncontrado("Recurso não encontrado.");
    }
}
