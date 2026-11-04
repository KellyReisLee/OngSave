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
import br.com.ongsave.dao.SenhaTokenDAO;
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

            // Hash antigo (menos iterações): atualiza de forma transparente, já que temos a senha correta em mãos.
            if (Senhas.precisaAtualizar(u.getSenhaHash())) usuarios.atualizarSenha(c, u.getId(), Senhas.gerar(senha));
            usuarios.registrarAcesso(c, u.getId());
            return new Resultado(u.paraSessao(), null);
        });
    }

    /**
     * Entrada sem senha nas contas de demonstração (página /demo). Se um visitante tiver suspendido a conta
     * ou errado a senha várias vezes, a conta volta a ficar ativa e desbloqueada para a demonstração seguir.
     */
    public Usuario entrarDemo(String email) {
        return Banco.transacao(c -> {
            Conta u = usuarios.buscarPorEmail(c, email, true);
            if (u == null)
                throw ErroNegocio.naoEncontrado("A conta de demonstração " + email + " não existe neste banco. "
                        + "Carregue os dados de demonstração (script ongsave_neon.sql completo ou app.dadosDemo=true num banco vazio).");
            if (!"ativo".equals(u.getStatus())) {
                usuarios.atualizarStatus(c, u.getId(), "ativo");
                u.setStatus("ativo");
            }
            usuarios.limparBloqueio(c, u.getId());
            usuarios.registrarAcesso(c, u.getId());
            return u.paraSessao();
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
        if (Senhas.fraca(d.senha())) throw ErroNegocio.invalido("Essa senha é muito comum. Escolha outra, com letras e números.");
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

    /* ======================= Recuperação de senha (link por e-mail) ======================= */

    private static final int TOKEN_MINUTOS = 30;
    private static final int MAX_PEDIDOS_HORA = 3;
    private final SenhaTokenDAO tokens = new SenhaTokenDAO();
    private static final java.security.SecureRandom RANDOM = new java.security.SecureRandom();

    /**
     * Passo 1: se o e-mail pertencer a uma conta ativa, gera um token de uso único (30 min) e envia o link.
     * A resposta para quem pede é SEMPRE a mesma (não revela se o e-mail tem conta). Só o hash do token vai para o banco.
     *
     * @param urlBase endereço público da aplicação, sem barra final (ex.: https://ongsave.onrender.com)
     */
    public void solicitarRecuperacao(String email, String urlBase, String ip, EmailService emails) {
        String mail = email == null ? "" : email.trim();
        if (mail.isEmpty() || mail.length() > 160 || !mail.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$"))
            throw ErroNegocio.invalido("Informe um e-mail válido.");
        byte[] b = new byte[32];
        RANDOM.nextBytes(b);
        String token = java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(b);

        Conta u = Banco.transacao(c -> {
            Conta conta = usuarios.buscarPorEmail(c, mail, true);
            if (conta == null || !"ativo".equals(conta.getStatus())) return null;
            if (tokens.pedidosRecentes(c, conta.getId()) >= MAX_PEDIDOS_HORA) return null;   // anti-spam, em silêncio
            tokens.inserir(c, conta.getId(), Senhas.sha256(token), TOKEN_MINUTOS, ip);
            auditoria.registrar(c, null, "Pediu recuperação de senha", conta.getNome(), conta.getId(), ip == null ? null : "IP " + ip);
            return conta;
        });
        if (u == null) return;

        String link = urlBase + "/recuperar-senha?token=" + token;
        String nome = u.getNome() == null ? "" : u.getNome();
        String texto = "Olá, " + nome + ".\n\nRecebemos um pedido para redefinir a senha da sua conta OngSave.\n"
                + "Abra o link abaixo (válido por " + TOKEN_MINUTOS + " minutos, uso único):\n\n" + link
                + "\n\nSe não foi você, ignore este e-mail: a sua senha continua a mesma.\n\nEquipa OngSave";
        String html = "<div style=\"font-family:Arial,sans-serif;max-width:520px;margin:auto;color:#0f172a\">"
                + "<h2 style=\"color:#ea580c\">OngSave · redefinir senha</h2>"
                + "<p>Olá, " + escaparHtml(nome) + ".</p><p>Recebemos um pedido para redefinir a senha da sua conta.</p>"
                + "<p><a href=\"" + escaparHtml(link) + "\" style=\"display:inline-block;background:#ea580c;color:#fff;"
                + "padding:12px 20px;border-radius:12px;text-decoration:none;font-weight:bold\">Criar nova senha</a></p>"
                + "<p style=\"font-size:13px;color:#64748b\">O link vale " + TOKEN_MINUTOS + " minutos e só pode ser usado uma vez. "
                + "Se não foi você, ignore este e-mail: a sua senha continua a mesma.</p></div>";
        emails.enviar(u.getEmail(), "OngSave · redefinir a sua senha", texto, html);
    }

    /** O link ainda é válido? (para mostrar o formulário ou um aviso de link expirado) */
    public boolean tokenValido(String token) {
        if (token == null || token.length() < 20 || token.length() > 100) return false;
        return Banco.ler(c -> tokens.usuarioDoTokenValido(c, Senhas.sha256(token)) != null);
    }

    /** Passo 2: troca a senha com o token. Invalida todos os links em aberto da conta e limpa o bloqueio. */
    public void redefinirComToken(String token, String nova, String confirmacao) {
        if (nova == null || nova.length() < 8 || nova.length() > 128)
            throw ErroNegocio.invalido("A nova senha precisa de 8 a 128 caracteres.");
        if (!nova.equals(confirmacao))
            throw ErroNegocio.invalido("As senhas não coincidem. Digite a mesma senha nos dois campos.");
        if (Senhas.fraca(nova))
            throw ErroNegocio.invalido("Essa senha é muito comum. Escolha outra, com letras e números.");
        if (token == null || token.length() < 20 || token.length() > 100)
            throw ErroNegocio.invalido("Link inválido ou expirado. Peça um novo link de recuperação.");
        Banco.transacao(c -> {
            Long id = tokens.usuarioDoTokenValido(c, Senhas.sha256(token));
            if (id == null) throw ErroNegocio.invalido("Link inválido ou expirado. Peça um novo link de recuperação.");
            Conta u = usuarios.buscarPorId(c, id, true);
            usuarios.atualizarSenha(c, id, Senhas.gerar(nova));
            usuarios.limparBloqueio(c, id);
            tokens.consumirTodos(c, id);
            auditoria.registrar(c, null, "Senha redefinida (link por e-mail)", u == null ? "Conta #" + id : u.getNome(), id, null);
            return null;
        });
    }

    private static String escaparHtml(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&#39;");
    }

    /** Troca de senha (usada pelo admin inicial e pelos perfis no futuro). */
    public void trocarSenha(Connection c, long id, String nova) throws SQLException {
        if (nova == null || nova.length() < 8) throw ErroNegocio.invalido("A senha precisa de pelo menos 8 caracteres.");
        usuarios.atualizarSenha(c, id, Senhas.gerar(nova));
    }

    /** Troca de senha pelo próprio utilizador, conferindo a senha atual. */
    public void alterarSenha(Usuario u, String atual, String nova) {
        if (nova == null || nova.length() < 8 || nova.length() > 128) throw ErroNegocio.invalido("A nova senha precisa de 8 a 128 caracteres.");
        if (Senhas.fraca(nova)) throw ErroNegocio.invalido("Essa senha é muito comum. Escolha outra, com letras e números.");
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
