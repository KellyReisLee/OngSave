package br.com.ongsave.dao;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import br.com.ongsave.db.Sql;
import br.com.ongsave.model.Documento;

/** Acesso à tabela documentos (documentos de cadastro e respetiva aprovação). */
public class DocumentoDAO {

    /** Documento sem o conteúdo do ficheiro. */
    public Documento buscar(Connection c, long id) throws SQLException {
        return documento(Sql.linha(c, "SELECT id, usuario_id, nome, arquivo_nome, content_type, status, motivo, atualizado_em "
                + "FROM documentos WHERE id = ?", id));
    }

    /** Documento com o ficheiro, ou null se ainda não foi enviado. */
    public Documento buscarComArquivo(Connection c, long id) throws SQLException {
        Documento d = documento(Sql.linha(c, "SELECT id, usuario_id, nome, arquivo_nome, content_type, status, motivo, atualizado_em "
                + "FROM documentos WHERE id = ? AND conteudo IS NOT NULL", id));
        if (d != null) d.setConteudo(Sql.bytes(c, "SELECT conteudo FROM documentos WHERE id = ?", id));
        return d;
    }

    /** Lista do perfil (id, nome, arq, status, motivo). */
    public List<Map<String, Object>> listarDoUsuario(Connection c, long usuarioId) throws SQLException {
        return Sql.lista(c, "SELECT id, nome, COALESCE(arquivo_nome, '—') AS arq, status, motivo FROM documentos "
                + "WHERE usuario_id = ? ORDER BY id", usuarioId);
    }

    /** Todos os documentos (painel do admin), com o dono e se já há ficheiro. */
    public List<Map<String, Object>> listarTodos(Connection c) throws SQLException {
        return Sql.lista(c, "SELECT id, usuario_id, nome, COALESCE(arquivo_nome, '—') AS arq, status, motivo, "
                + "conteudo IS NOT NULL AS arquivo FROM documentos ORDER BY id");
    }

    public boolean temPendentes(Connection c, long usuarioId) throws SQLException {
        return Sql.numero(c, "SELECT count(*) FROM documentos WHERE usuario_id = ? AND status <> 'ok'", usuarioId) > 0;
    }

    public void inserir(Connection c, Documento d) throws SQLException {
        Sql.executar(c, "INSERT INTO documentos (usuario_id, nome, status) VALUES (?, ?, ?)", d.getUsuarioId(), d.getNome(), d.getStatus());
    }

    /** Novo ficheiro enviado: volta para "em análise". */
    public void registrarEnvio(Connection c, long id, String arquivoNome, byte[] conteudo, String contentType) throws SQLException {
        Sql.executar(c, "UPDATE documentos SET arquivo_nome = ?, conteudo = ?, content_type = ?, status = 'analise', motivo = NULL, "
                + "atualizado_em = now() WHERE id = ?", arquivoNome, conteudo, contentType, id);
    }

    public void avaliar(Connection c, long id, String status, String motivo) throws SQLException {
        Sql.executar(c, "UPDATE documentos SET status = ?, motivo = ?, atualizado_em = now() WHERE id = ?", status, motivo, id);
    }

    /** O admin pede um novo envio de um documento que o utilizador já tem na lista. */
    public void solicitarNovoEnvio(Connection c, long usuarioId, String nome) throws SQLException {
        Sql.executar(c, "UPDATE documentos SET status = 'rejeitado', motivo = 'Novo envio solicitado' WHERE usuario_id = ? AND nome = ?", usuarioId, nome);
    }

    private static Documento documento(Map<String, Object> m) {
        if (m == null) return null;
        Documento d = new Documento();
        d.setId(Linhas.l(m, "id"));
        d.setUsuarioId(Linhas.l(m, "usuario_id"));
        d.setNome(Linhas.s(m, "nome"));
        d.setArquivoNome(Linhas.s(m, "arquivo_nome"));
        d.setContentType(Linhas.s(m, "content_type"));
        d.setStatus(Linhas.s(m, "status"));
        d.setMotivo(Linhas.s(m, "motivo"));
        d.setAtualizadoEm(Linhas.instante(m, "atualizado_em"));
        return d;
    }
}
