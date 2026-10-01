package br.com.ongsave.service;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import br.com.ongsave.dao.NotificacaoDAO;
import br.com.ongsave.db.Banco;
import br.com.ongsave.model.Notificacao;
import br.com.ongsave.util.Json;
import br.com.ongsave.util.Tempo;

/** Notificações internas (sino do topo dos painéis). */
public class NotificacaoService {

    private final NotificacaoDAO dao = new NotificacaoDAO();

    public void notificar(Connection c, Long usuarioId, String texto, String icone) throws SQLException {
        if (usuarioId == null) return;
        Notificacao n = new Notificacao();
        n.setUsuarioId(usuarioId);
        n.setTexto(AuditoriaService.corta(texto, 300));
        n.setIcone(icone == null ? "fa-bell" : icone);
        dao.inserir(c, n);
    }

    public void notificarAdmins(Connection c, String texto, String icone) throws SQLException {
        dao.inserirParaAdmins(c, AuditoriaService.corta(texto, 300), icone == null ? "fa-bell" : icone);
    }

    /** Últimas 30 notificações no formato do front: {id, t, ic, lida, q}. */
    public List<Map<String, Object>> listar(Connection c, long usuarioId) throws SQLException {
        List<Map<String, Object>> r = new ArrayList<>();
        for (Notificacao n : dao.listarRecentes(c, usuarioId, 30))
            r.add(Json.obj("id", n.getId(), "t", n.getTexto(), "ic", n.getIcone(), "lida", n.isLida(), "q", Tempo.relativo(n.getCriadoEm())));
        return r;
    }

    public void marcarLidas(long usuarioId) {
        Banco.transacao(c -> { dao.marcarLidas(c, usuarioId); return null; });
        // Mantém a tabela enxuta: só as 200 mais recentes por utilizador.
        Banco.transacao(c -> { dao.manterRecentes(c, usuarioId, 200); return null; });
    }
}
