-- OngSave · V004 · Recuperação de senha por link enviado ao e-mail.
-- Substitui a verificação por CPF/CNPJ/telefone (dados públicos ou fáceis de obter).
-- Só o hash SHA-256 do token é guardado: quem lê o banco não consegue usar os links.
CREATE TABLE IF NOT EXISTS senha_tokens (
    id           BIGSERIAL    PRIMARY KEY,
    usuario_id   BIGINT       NOT NULL REFERENCES usuarios(id) ON DELETE CASCADE,
    token_hash   CHAR(64)     NOT NULL UNIQUE,
    criado_em    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    expira_em    TIMESTAMPTZ  NOT NULL,
    usado_em     TIMESTAMPTZ,
    ip_pedido    VARCHAR(45)
);
CREATE INDEX IF NOT EXISTS ix_senha_tokens_usuario ON senha_tokens (usuario_id, criado_em DESC);
