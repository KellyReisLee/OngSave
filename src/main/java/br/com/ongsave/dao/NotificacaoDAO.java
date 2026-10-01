package br.com.ongsave.dao;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import br.com.ongsave.db.Sql;
import br.com.ongsave.model.Notificacao;

/** Acesso à tabela notificacoes (sino do topo dos painéis). */
public class NotificacaoDAO {

    public void inserir(Connection c, Notificacao n) throws SQLException {
        Sql.executar(c, "INSERT INTO notificacoes (usuario_id, texto, icone) VALUES (?, ?, ?)", n.getUsuarioId(), n.getTexto(), n.getIcone());
    }

    /** A mesma notificação para todos os administradores ativos. */
    public void inserirParaAdmins(Connection c, String texto, String icone) throws SQLException {
        Sql.executar(c, "INSERT INTO notificacoes (usuario_id, texto, icone) "
                + "SELECT id, ?, ? FROM usuarios WHERE perfil = 'admin' AND status = 'ativo'", texto, icone);
    }

    /** Avisa as empresas dos lotes que venceram sem motorista (antes de serem cancelados). */
    public void avisarLotesVencidos(Connection c) throws SQLException {
        Sql.executar(c, """
                INSERT INTO notificacoes (usuario_id, texto, icone)
                SELECT empresa_id, 'O lote #' || id || ' venceu sem motorista e foi encerrado automaticamente.', 'fa-hourglass-end'
                FROM lotes WHERE estado = 'aguardando' AND validade < now()""");
    }

    public List<Notificacao> listarRecentes(Connection c, long usuarioId, int limite) throws SQLException {
        List<Notificacao> r = new ArrayList<>();
        for (Map<String, Object> m : Sql.lista(c, "SELECT id, usuario_id, texto, icone, lida, criado_em FROM notificacoes "
                + "WHERE usuario_id = ? ORDER BY criado_em DESC LIMIT ?", usuarioId, limite)) {
            Notificacao n = new Notificacao();
            n.setId(Linhas.l(m, "id"));
            n.setUsuarioId(Linhas.l(m, "usuario_id"));
            n.setTexto(Linhas.s(m, "texto"));
            n.setIcone(Linhas.s(m, "icone"));
            n.setLida(Linhas.b(m, "lida"));
            n.setCriadoEm(Linhas.instante(m, "criado_em"));
            r.add(n);
        }
        return r;
    }

    public void marcarLidas(Connection c, long usuarioId) throws SQLException {
        Sql.executar(c, "UPDATE notificacoes SET lida = TRUE WHERE usuario_id = ? AND NOT lida", usuarioId);
    }

    /** Mantém só as {@code maximo} notificações mais recentes do utilizador. */
    public void manterRecentes(Connection c, long usuarioId, int maximo) throws SQLException {
        Sql.executar(c, "DELETE FROM notificacoes WHERE usuario_id = ? AND id NOT IN "
                + "(SELECT id FROM notificacoes WHERE usuario_id = ? ORDER BY criado_em DESC LIMIT ?)", usuarioId, usuarioId, maximo);
    }

    public int apagarLidasAntigas(Connection c, int dias) throws SQLException {
        return Sql.executar(c, "DELETE FROM notificacoes WHERE lida AND criado_em < now() - (? * interval '1 day')", dias);
    }
}
