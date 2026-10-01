package br.com.ongsave.service;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import br.com.ongsave.dao.DocumentoDAO;
import br.com.ongsave.dao.EmpresaDAO;
import br.com.ongsave.dao.MotoristaDAO;
import br.com.ongsave.dao.OngDAO;
import br.com.ongsave.dao.UsuarioDAO;
import br.com.ongsave.db.Banco;
import br.com.ongsave.model.Conta;
import br.com.ongsave.model.Documento;
import br.com.ongsave.model.Empresa;
import br.com.ongsave.model.ErroNegocio;
import br.com.ongsave.model.Motorista;
import br.com.ongsave.model.Ong;
import br.com.ongsave.model.Perfil;
import br.com.ongsave.model.Usuario;
import br.com.ongsave.util.Documentos;
import br.com.ongsave.util.Senhas;

/**
 * Login e cadastro (contas em {@link UsuarioDAO} + dados do perfil no DAO de cada perfil).
 * Proteções: hash PBKDF2, tempo constante para e-mails inexistentes e bloqueio
 * temporário após 5 senhas erradas seguidas.
 */
public class AutenticacaoService {

    private static final int MAX_FALHAS = 5;
    private static final int BLOQUEIO_MIN = 15;

    /** Documentos pedidos a cada perfil no cadastro (o admin aprova um a um). */
    public static final Map<Perfil, List<String>> DOCUMENTOS = Map.of(
            Perfil.EMPRESA, List.of("Contrato social e CNPJ", "Termo de responsabilidade técnica", "Comprovante de endereço"),
            Perfil.MOTORISTA, List.of("CNH", "CRLV do veículo", "Comprovante de residência", "Foto do veículo"),
            Perfil.ONG, List.of("Alvará / registro da instituição", "Estatuto e ata da diretoria", "Comprovante de endereço", "Documento do responsável"));

    private final NotificacaoService notificacoes;
    private final AuditoriaService auditoria;
    private final UsuarioDAO usuarios = new UsuarioDAO();
    private final EmpresaDAO empresas = new EmpresaDAO();
    private final MotoristaDAO motoristas = new MotoristaDAO();
    private final OngDAO ongs = new OngDAO();
    private final DocumentoDAO documentos = new DocumentoDAO();

    public AutenticacaoService(NotificacaoService notificacoes, AuditoriaService auditoria) {
        this.notificacoes = notificacoes;
        this.auditoria = auditoria;
    }

    public record Resultado(Usuario usuario, String erro) {}

    public Resultado autenticar(String email, String senha) {
        if (email == null || email.isBlank() || senha == null || senha.isEmpty())
            return new Resultado(null, "Informe e-mail e senha.");
        return Banco.transacao(c -> {
            Conta u = usuarios.buscarPorEmail(c, email, true);
            if (u == null) {
                Senhas.gastarTempo(senha);
                return new Resultado(null, "E-mail ou senha incorretos.");
            }
            if (u.isBloqueadaTemporariamente()) {
                long min = Math.max(1, (u.getBloqueioAte().getEpochSecond() - Instant.now().getEpochSecond() + 59) / 60);
                return new Resultado(null, "Muitas tentativas sem sucesso. Tente de novo em " + min + " minuto(s).");
            }
            if (!Senhas.confere(senha, u.getSenhaHash())) {
                int falhas = u.getFalhasLogin() + 1;
                if (falhas >= MAX_FALHAS) usuarios.bloquearTemporariamente(c, u.getId(), BLOQUEIO_MIN);
                else usuarios.registrarFalhaLogin(c, u.getId(), falhas);
                return new Resultado(null, "E-mail ou senha incorretos.");
            }
            String erroStatus = switch (u.getStatus()) {
                case "pendente" -> "O seu cadastro está em análise pela equipa OngSave. Assim que for aprovado, poderá entrar.";
                case "rejeitado" -> "O cadastro não foi aprovado. Fale com o suporte da OngSave para saber o motivo.";
                case "suspenso" -> "Conta suspensa. Consulte o motivo com o suporte e, se discordar, conteste a decisão.";
                case "bloqueado" -> "Conta bloqueada. Fale com o suporte da OngSave.";
                default -> null;
            };
            if (erroStatus != null) return new Resultado(null, erroStatus);

            usuarios.registrarAcesso(c, u.getId());
            return new Resultado(u.paraSessao(), null);
        });
    }

    public boolean emailEmUso(String email) {
        return Banco.ler(c -> usuarios.emailExiste(c, email));
    }

    /** Status atual da conta (para derrubar sessões de contas suspensas). */
    public String status(long id) {
        return Banco.ler(c -> usuarios.status(c, id));
    }

    /** Dados recebidos do formulário de cadastro (já sem espaços nas pontas). */
    public record Cadastro(Perfil perfil, String nome, String email, String senha, String documento, String telefone,
                           String extra, String cnh, String categoriaCnh, String placa) {}

    /** Valida e cria a conta como "pendente". Lança {@link ErroNegocio} com mensagem para o utilizador. */
    public long registrar(Cadastro d) {
        validar(d);
        return Banco.transacao(c -> {
            if (usuarios.emailExiste(c, d.email()))
                throw ErroNegocio.conflito("Este e-mail já está cadastrado.");
            String doc = d.perfil() == Perfil.MOTORISTA ? Documentos.formatarCpf(d.documento()) : Documentos.formatarCnpj(d.documento());
            if (usuarios.documentoExiste(c, doc, d.perfil()))
                throw ErroNegocio.conflito("Já existe uma conta com este " + (d.perfil() == Perfil.MOTORISTA ? "CPF." : "CNPJ."));

            Conta conta = new Conta();
            conta.setPerfil(d.perfil());
            conta.setNome(d.nome());
            conta.setEmail(d.email().toLowerCase(Locale.ROOT));
            conta.setSenhaHash(Senhas.gerar(d.senha()));
            conta.setStatus("pendente");
            conta.setDocumento(doc);
            conta.setTelefone(d.telefone());
            long id = usuarios.inserir(c, conta);

            switch (d.perfil()) {
                case EMPRESA -> {
                    Empresa e = new Empresa();
                    e.setUsuarioId(id);
                    e.setSetor(d.extra());
                    e.setResponsavel(d.nome());
                    empresas.inserirCadastro(c, e);
                }
                case MOTORISTA -> {
                    Motorista m = new Motorista();
                    m.setUsuarioId(id);
                    m.setPlaca(Documentos.placa(d.placa()));
                    m.setCnh(Documentos.digitos(d.cnh()));
                    m.setCnhCategoria(d.categoriaCnh());
                    m.setCnhValidade(LocalDate.now().plusYears(1));
                    motoristas.inserirCadastro(c, m);
                }
                case ONG -> {
                    Ong o = new Ong();
                    o.setUsuarioId(id);
                    o.setRazaoSocial(d.nome());
                    o.setResponsavel(d.nome());
                    o.setFamilias(primeiroNumero(d.extra()));
                    o.setAlvaraValidade(LocalDate.now().plusYears(1));
                    ongs.inserirCadastro(c, o);
                }
                default -> throw ErroNegocio.proibido("Perfil não permitido.");
            }
            for (String nomeDoc : DOCUMENTOS.get(d.perfil())) {
                Documento doc1 = new Documento();
                doc1.setUsuarioId(id);
                doc1.setNome(nomeDoc);
                doc1.setStatus("analise");
                documentos.inserir(c, doc1);
            }

            notificacoes.notificarAdmins(c, "Novo cadastro: " + d.nome() + " (" + d.perfil().getDescricao().toLowerCase(Locale.ROOT) + ") aguarda aprovação.", "fa-user-plus");
            auditoria.registrar(c, null, "Novo cadastro", d.nome(), id, d.perfil().getDescricao());
            return id;
        });
    }

    private static void validar(Cadastro d) {
        if (d.perfil() == null) throw ErroNegocio.invalido("Escolha um perfil.");
        if (d.perfil() == Perfil.ADMIN) throw ErroNegocio.proibido("Contas de administrador são criadas internamente pela equipa OngSave.");
        if (d.nome().length() < 3 || d.nome().length() > 160) throw ErroNegocio.invalido("Preencha o nome (3 a 160 caracteres).");
        if (!d.email().matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$") || d.email().length() > 160) throw ErroNegocio.invalido("Informe um e-mail válido.");
        if (d.senha() == null || d.senha().length() < 8 || d.senha().length() > 128) throw ErroNegocio.invalido("A senha precisa de 8 a 128 caracteres.");
        if (d.perfil() == Perfil.MOTORISTA) {
            if (!Documentos.cpfValido(d.documento())) throw ErroNegocio.invalido("Informe um CPF válido.");
            if (Documentos.digitos(d.cnh()).length() != 11) throw ErroNegocio.invalido("O número da CNH tem 11 dígitos.");
            if (!List.of("B", "C", "D", "E").contains(d.categoriaCnh())) throw ErroNegocio.invalido("Escolha a categoria da CNH.");
            if (Documentos.placa(d.placa()) == null) throw ErroNegocio.invalido("Informe a placa no formato ABC1D23 ou ABC1234.");
        } else if (!Documentos.cnpjValido(d.documento())) {
            throw ErroNegocio.invalido("Informe um CNPJ válido.");
        }
        if (d.telefone() != null && !d.telefone().isBlank() && !Documentos.telefoneValido(d.telefone()))
            throw ErroNegocio.invalido("Informe um telefone com DDD.");
        if (d.perfil() != Perfil.MOTORISTA && (d.extra() == null || d.extra().isBlank()))
            throw ErroNegocio.invalido(d.perfil() == Perfil.ONG ? "Informe o número de famílias atendidas." : "Informe o setor de atuação.");
    }

    private static int primeiroNumero(String s) {
        String d = s == null ? "" : s.replaceAll("^\\D*(\\d+).*$", "$1");
        try { return d.matches("\\d+") ? Math.min(Integer.parseInt(d), 1_000_000) : 0; } catch (NumberFormatException e) { return 0; }
    }

    /**
     * Recuperação de senha simples, sem e-mail nem código: a pessoa informa o e-mail da conta,
     * o CPF/CNPJ (ou o telefone) cadastrado e a nova senha duas vezes.
     *
     * <p>Proteções mínimas: a mesma mensagem para e-mail inexistente ou dados errados (não revela
     * quem tem conta), tempo constante e o mesmo bloqueio de 15 minutos após 5 tentativas erradas
     * usado no login. Ao trocar com sucesso, o contador de falhas e o bloqueio são limpos.</p>
     */
    public void redefinirSenha(String email, String verificacao, String nova, String confirmacao) {
        String mail = email == null ? "" : email.trim();
        String digitosInformados = Documentos.digitos(verificacao == null ? "" : verificacao);
        if (mail.isEmpty() || digitosInformados.isEmpty())
            throw ErroNegocio.invalido("Informe o e-mail e o CPF/CNPJ (ou telefone) da conta.");
        if (nova == null || nova.length() < 8 || nova.length() > 128)
            throw ErroNegocio.invalido("A nova senha precisa de 8 a 128 caracteres.");
        if (!nova.equals(confirmacao))
            throw ErroNegocio.invalido("As senhas não coincidem. Digite a mesma senha nos dois campos.");

        // Resultado devolvido em vez de exceção para que o registo da tentativa falhada seja confirmado (commit).
        String erro = Banco.transacao(c -> {
            Conta u = usuarios.buscarPorEmail(c, mail, true);
            if (u == null) {
                Senhas.gastarTempo(nova);
                return "Não encontrámos uma conta com esses dados. Confira o e-mail e o CPF/CNPJ (ou telefone).";
            }
            if (u.isBloqueadaTemporariamente()) {
                long min = Math.max(1, (u.getBloqueioAte().getEpochSecond() - Instant.now().getEpochSecond() + 59) / 60);
                return "Muitas tentativas sem sucesso. Tente de novo em " + min + " minuto(s).";
            }
            boolean confere = digitosInformados.equals(Documentos.digitos(nvl(u.getDocumento())))
                    || digitosInformados.equals(Documentos.digitos(nvl(u.getTelefone())));
            if (!confere) {
                Senhas.gastarTempo(nova);
                int falhas = u.getFalhasLogin() + 1;
                if (falhas >= MAX_FALHAS) usuarios.bloquearTemporariamente(c, u.getId(), BLOQUEIO_MIN);
                else usuarios.registrarFalhaLogin(c, u.getId(), falhas);
                return "Não encontrámos uma conta com esses dados. Confira o e-mail e o CPF/CNPJ (ou telefone).";
            }
            usuarios.atualizarSenha(c, u.getId(), Senhas.gerar(nova));
            usuarios.limparBloqueio(c, u.getId());
            auditoria.registrar(c, null, "Senha redefinida (recuperação)", u.getNome(), u.getId(), null);
            return null;
        });
        if (erro != null) throw ErroNegocio.invalido(erro);
    }

    private static String nvl(String s) {
        return s == null ? "" : s;
    }

    /** Troca de senha (usada pelo admin inicial e pelos perfis no futuro). */
    public void trocarSenha(Connection c, long id, String nova) throws SQLException {
        if (nova == null || nova.length() < 8) throw ErroNegocio.invalido("A senha precisa de pelo menos 8 caracteres.");
        usuarios.atualizarSenha(c, id, Senhas.gerar(nova));
    }

    /** Troca de senha pelo próprio utilizador, conferindo a senha atual. */
    public void alterarSenha(Usuario u, String atual, String nova) {
        if (nova == null || nova.length() < 8 || nova.length() > 128) throw ErroNegocio.invalido("A nova senha precisa de 8 a 128 caracteres.");
        Banco.transacao(c -> {
            Conta conta = usuarios.buscarPorId(c, u.getId(), true);
            String hash = conta == null ? null : conta.getSenhaHash();
            if (hash == null || atual == null || !Senhas.confere(atual, hash)) throw ErroNegocio.invalido("A senha atual não confere.");
            trocarSenha(c, u.getId(), nova);
            auditoria.registrar(c, u, "Alterou a senha", u.getNome(), u.getId(), null);
            return null;
        });
    }
}
