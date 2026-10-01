package br.com.ongsave.service;

import br.com.ongsave.dao.LoteDAO;
import br.com.ongsave.db.Banco;
import br.com.ongsave.model.ErroNegocio;
import br.com.ongsave.model.Lote;
import br.com.ongsave.model.Usuario;

/** Fotos dos lotes (publicação e prova de entrega), entregues só a quem participa do lote ou ao admin. */
public class ArquivosService {

    public record Arquivo(byte[] bytes, String tipo) {}

    private final LoteDAO lotes = new LoteDAO();

    public Arquivo fotoLote(Usuario u, long loteId, boolean entrega) {
        return Banco.ler(c -> {
            Lote l = lotes.buscarPorId(c, loteId, false);
            if (l == null) throw ErroNegocio.naoEncontrado("Lote não encontrado.");
            if (!l.participa(u)) throw ErroNegocio.proibido("Sem acesso a esta foto.");
            byte[] b = lotes.lerFoto(c, loteId, entrega);
            if (b == null) throw ErroNegocio.naoEncontrado("Foto não encontrada.");
            return new Arquivo(b, entrega ? l.getEntregaFotoTipo() : l.getFotoTipo());
        });
    }
}
