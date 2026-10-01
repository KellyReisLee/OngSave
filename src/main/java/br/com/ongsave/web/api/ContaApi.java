package br.com.ongsave.web.api;

import java.io.IOException;

import br.com.ongsave.model.ErroNegocio;
import br.com.ongsave.model.Perfil;
import br.com.ongsave.service.ArquivosService;
import br.com.ongsave.service.OngService;
import br.com.ongsave.util.Json;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;

/**
 * Rotas comuns a todos os perfis.
 * <pre>
 * GET  /api/conta/plataforma                 parâmetros da plataforma (leitura)
 * GET  /api/conta/fotos/lote/{id}            foto publicada pela empresa
 * GET  /api/conta/fotos/entrega/{id}         foto da descarga enviada pelo motorista
 * POST /api/conta/notificacoes/lidas
 * POST /api/conta/documentos/{id}            multipart: arquivo (PDF/PNG/JPG até 5 MB)
 * POST /api/conta/senha                      atual, nova
 * </pre>
 */
@WebServlet(name = "ContaApi", urlPatterns = "/api/conta/*")
@MultipartConfig(maxFileSize = 6 * 1024 * 1024, maxRequestSize = 7 * 1024 * 1024, fileSizeThreshold = 512 * 1024)
public class ContaApi extends ApiBase {
    private static final long serialVersionUID = 1L;
    private final ArquivosService arquivos = new ArquivosService();

    @Override
    protected Perfil perfil() { return null; }

    @Override
    protected Object get(Pedido p) {
        if (p.e("plataforma")) return p.s.plataforma.config();
        if (p.e("fotos", "lote", "*") || p.e("fotos", "entrega", "*")) {
            ArquivosService.Arquivo a = arquivos.fotoLote(p.usuario, p.id(2), "entrega".equals(p.rota.get(1)));
            return new Arquivo(a.bytes(), a.tipo(), null);
        }
        throw ErroNegocio.naoEncontrado("Recurso não encontrado.");
    }

    @Override
    protected Object post(Pedido p) throws IOException, ServletException {
        if (p.e("notificacoes", "lidas")) { p.s.notificacoes.marcarLidas(p.usuario.getId()); return null; }
        if (p.e("documentos", "*")) {
            return Json.obj("docs", OngService.enviarDocumentoComum(p.usuario, p.id(1), p.nomeArquivo("arquivo"),
                    p.arquivo("arquivo", 5 * 1024 * 1024), p.s.notificacoes, p.s.auditoria));
        }
        if (p.e("senha")) { p.s.autenticacao.alterarSenha(p.usuario, p.p("atual"), p.p("nova")); return null; }
        throw ErroNegocio.naoEncontrado("Recurso não encontrado.");
    }
}
