package br.com.ongsave.service;

import java.sql.Connection;
import java.sql.SQLException;

import br.com.ongsave.dao.AuditoriaDAO;
import br.com.ongsave.model.RegistroAuditoria;
import br.com.ongsave.model.Usuario;

/** Trilha de auditoria: toda ação sensível fica registada com autor, alvo e justificativa. */
public class AuditoriaService {

    private final AuditoriaDAO dao = new AuditoriaDAO();

    public void registrar(Connection c, Usuario ator, String acao, String alvo, Long alvoId, String extra) throws SQLException {
        RegistroAuditoria r = new RegistroAuditoria();
        r.setAtorId(ator == null ? null : ator.getId());
        r.setAtorNome(ator == null ? "Sistema" : ator.getNome());
        r.setAcao(corta(acao, 120));
        r.setAlvo(corta(alvo, 160));
        r.setAlvoId(alvoId);
        r.setExtra(corta(extra, 600));
        dao.inserir(c, r);
    }

    static String corta(String s, int max) {
        if (s == null) return null;
        return s.length() <= max ? s : s.substring(0, max);
    }
}
