-- =====================================================================
-- OngSave · V001 · Esquema inicial (PostgreSQL 15+ / Neon)
-- Aplicado automaticamente no arranque pelo Migrador. NÃO edite depois
-- de aplicado em produção: crie um V00X__descricao.sql novo.
-- =====================================================================

-- ---------- Contas ----------
CREATE TABLE usuarios (
    id                BIGSERIAL    PRIMARY KEY,
    perfil            VARCHAR(12)  NOT NULL CHECK (perfil IN ('empresa','motorista','ong','admin')),
    nome              VARCHAR(160) NOT NULL,
    email             VARCHAR(160) NOT NULL,
    senha_hash        VARCHAR(200) NOT NULL,
    status            VARCHAR(12)  NOT NULL DEFAULT 'pendente'
                      CHECK (status IN ('pendente','ativo','suspenso','bloqueado','rejeitado')),
    documento         VARCHAR(20),
    telefone          VARCHAR(20),
    cidade            VARCHAR(80)  NOT NULL DEFAULT 'São Paulo/SP',
    criado_em         TIMESTAMPTZ  NOT NULL DEFAULT now(),
    ultimo_acesso     TIMESTAMPTZ,
    falhas_login      INT          NOT NULL DEFAULT 0,
    bloqueio_ate      TIMESTAMPTZ
);
CREATE UNIQUE INDEX ux_usuarios_email ON usuarios (lower(email));
CREATE INDEX ix_usuarios_perfil_status ON usuarios (perfil, status);

CREATE TABLE empresas (
    usuario_id      BIGINT PRIMARY KEY REFERENCES usuarios(id) ON DELETE CASCADE,
    setor           VARCHAR(120),
    responsavel     VARCHAR(120),
    plano           VARCHAR(10)  NOT NULL DEFAULT 'media' CHECK (plano IN ('pequena','media','grande')),
    custo_descarte  NUMERIC(12,2),
    cep             VARCHAR(9),
    rua             VARCHAR(160),
    numero          VARCHAR(20),
    complemento     VARCHAR(80),
    bairro          VARCHAR(80),
    cidade          VARCHAR(80),
    uf              CHAR(2),
    lat             DOUBLE PRECISION,
    lon             DOUBLE PRECISION
);

CREATE TABLE motoristas (
    usuario_id        BIGINT PRIMARY KEY REFERENCES usuarios(id) ON DELETE CASCADE,
    veiculo           VARCHAR(20)  NOT NULL DEFAULT 'Carro Económico'
                      CHECK (veiculo IN ('Carro Económico','Camionete','Van','Caminhão')),
    placa             VARCHAR(8),
    modelo            VARCHAR(80),
    cnh               VARCHAR(11),
    cnh_categoria     VARCHAR(2),
    cnh_validade      DATE,
    cep               VARCHAR(9),
    rua               VARCHAR(160),
    numero            VARCHAR(20),
    bairro            VARCHAR(80),
    cidade            VARCHAR(80),
    uf                CHAR(2),
    consentimento_em  TIMESTAMPTZ
);

CREATE TABLE ongs (
    usuario_id        BIGINT PRIMARY KEY REFERENCES usuarios(id) ON DELETE CASCADE,
    razao_social      VARCHAR(160),
    responsavel       VARCHAR(120),
    familias          INT          NOT NULL DEFAULT 0,
    capacidade_kg     NUMERIC(10,1) NOT NULL DEFAULT 300,
    hora_inicio       TIME         NOT NULL DEFAULT '08:00',
    hora_fim          TIME         NOT NULL DEFAULT '17:00',
    camara_fria       BOOLEAN      NOT NULL DEFAULT FALSE,
    categorias        TEXT[]       NOT NULL DEFAULT ARRAY[
                        'Laticínios e Frios','Hortifrúti / Frutas e Verduras','Padaria e Panificados',
                        'Mercearia / Não Perecíveis','Pratos Prontos / Marmitas'],
    alvara_validade   DATE,
    cep               VARCHAR(9),
    rua               VARCHAR(160),
    numero            VARCHAR(20),
    complemento       VARCHAR(80),
    bairro            VARCHAR(80),
    cidade            VARCHAR(80),
    uf                CHAR(2),
    lat               DOUBLE PRECISION,
    lon               DOUBLE PRECISION
);

CREATE TABLE documentos (
    id            BIGSERIAL PRIMARY KEY,
    usuario_id    BIGINT       NOT NULL REFERENCES usuarios(id) ON DELETE CASCADE,
    nome          VARCHAR(120) NOT NULL,
    arquivo_nome  VARCHAR(200),
    conteudo      BYTEA,
    content_type  VARCHAR(80),
    status        VARCHAR(10)  NOT NULL DEFAULT 'analise' CHECK (status IN ('ok','analise','rejeitado')),
    motivo        VARCHAR(200),
    atualizado_em TIMESTAMPTZ  NOT NULL DEFAULT now(),
    UNIQUE (usuario_id, nome)
);

CREATE TABLE notas_admin (
    id          BIGSERIAL PRIMARY KEY,
    usuario_id  BIGINT      NOT NULL REFERENCES usuarios(id) ON DELETE CASCADE,
    autor_id    BIGINT      REFERENCES usuarios(id) ON DELETE SET NULL,
    texto       VARCHAR(1000) NOT NULL,
    criado_em   TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX ix_notas_usuario ON notas_admin (usuario_id, criado_em DESC);

-- ---------- Parâmetros da plataforma (linha única, JSON editado pelo admin) ----------
CREATE TABLE plataforma_config (
    id             SMALLINT    PRIMARY KEY DEFAULT 1 CHECK (id = 1),
    dados          JSONB       NOT NULL,
    atualizado_em  TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- ---------- Lotes (doações) ----------
CREATE TABLE lotes (
    id                  BIGSERIAL PRIMARY KEY,
    empresa_id          BIGINT       NOT NULL REFERENCES usuarios(id),
    ong_id              BIGINT       REFERENCES usuarios(id),
    motorista_id        BIGINT       REFERENCES usuarios(id),
    categoria           VARCHAR(80)  NOT NULL,
    peso_kg             NUMERIC(10,1) NOT NULL CHECK (peso_kg > 0),
    volumes             INT          NOT NULL DEFAULT 1 CHECK (volumes > 0),
    conservacao         VARCHAR(4)   NOT NULL DEFAULT 'amb' CHECK (conservacao IN ('amb','ref','cong')),
    veiculo             VARCHAR(5)   NOT NULL CHECK (veiculo IN ('carro','van','cam')),
    validade            TIMESTAMPTZ  NOT NULL,
    janela_inicio       TIME,
    janela_fim          TIME,
    observacao          VARCHAR(500),
    foto                BYTEA,
    foto_tipo           VARCHAR(40),
    estado              VARCHAR(10)  NOT NULL DEFAULT 'aguardando'
                        CHECK (estado IN ('aguardando','aceito','transito','entregue','cancelado')),
    frete               NUMERIC(10,2),
    criado_em           TIMESTAMPTZ  NOT NULL DEFAULT now(),
    ong_aceite_em       TIMESTAMPTZ,
    aceito_em           TIMESTAMPTZ,
    cancelado_em        TIMESTAMPTZ,
    -- retirada registada pela empresa (gera o código que o motorista digita)
    retirada_codigo     VARCHAR(8),
    retirada_kg         NUMERIC(10,1),
    retirada_temp       NUMERIC(5,1),
    retirada_obs        VARCHAR(200),
    retirada_em         TIMESTAMPTZ,
    coleta_em           TIMESTAMPTZ,
    -- token de validação gerado pela ONG
    token_codigo        VARCHAR(8),
    token_gerado_em     TIMESTAMPTZ,
    token_expira_em     TIMESTAMPTZ,
    token_usado_em      TIMESTAMPTZ,
    token_tentativas    INT          NOT NULL DEFAULT 0,
    -- prova de entrega enviada pelo motorista
    entrega_foto        BYTEA,
    entrega_foto_tipo   VARCHAR(40),
    entrega_lat         DOUBLE PRECISION,
    entrega_lon         DOUBLE PRECISION,
    entrega_dist_m      INT,
    foto_capturada_em   TIMESTAMPTZ,
    entregue_em         TIMESTAMPTZ,
    -- conferência da ONG
    conf_kg             NUMERIC(10,1),
    conf_condicao       VARCHAR(10)  CHECK (conf_condicao IN ('ok','parcial','improprio')),
    conf_obs            VARCHAR(300),
    conf_div_pct        NUMERIC(6,1),
    conf_em             TIMESTAMPTZ,
    frete_retido        BOOLEAN      NOT NULL DEFAULT FALSE
);
CREATE INDEX ix_lotes_empresa ON lotes (empresa_id, criado_em DESC);
CREATE INDEX ix_lotes_ong ON lotes (ong_id, estado);
CREATE INDEX ix_lotes_motorista ON lotes (motorista_id, estado);
CREATE INDEX ix_lotes_estado ON lotes (estado, validade);
-- Um motorista só pode ter UMA entrega ativa (garantido pelo banco, não só pela aplicação)
CREATE UNIQUE INDEX ux_lote_motorista_ativo ON lotes (motorista_id) WHERE estado IN ('aceito','transito');

CREATE TABLE lote_recusas (
    lote_id    BIGINT NOT NULL REFERENCES lotes(id) ON DELETE CASCADE,
    ong_id     BIGINT NOT NULL REFERENCES usuarios(id) ON DELETE CASCADE,
    criado_em  TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (lote_id, ong_id)
);

-- ---------- Rastreamento (só durante a entrega ativa, com consentimento) ----------
CREATE TABLE posicoes (
    id            BIGSERIAL PRIMARY KEY,
    lote_id       BIGINT           NOT NULL REFERENCES lotes(id) ON DELETE CASCADE,
    motorista_id  BIGINT           NOT NULL REFERENCES usuarios(id) ON DELETE CASCADE,
    lat           DOUBLE PRECISION NOT NULL,
    lon           DOUBLE PRECISION NOT NULL,
    precisao_m    REAL,
    registado_em  TIMESTAMPTZ      NOT NULL DEFAULT now()
);
CREATE INDEX ix_posicoes_lote ON posicoes (lote_id, registado_em DESC);

-- ---------- Carteira do motorista ----------
CREATE TABLE movimentos (
    id            BIGSERIAL PRIMARY KEY,
    motorista_id  BIGINT        NOT NULL REFERENCES usuarios(id) ON DELETE CASCADE,
    lote_id       BIGINT        REFERENCES lotes(id) ON DELETE SET NULL,
    tipo          VARCHAR(8)    NOT NULL CHECK (tipo IN ('frete','saque','ajuste')),
    descricao     VARCHAR(200)  NOT NULL,
    valor         NUMERIC(12,2) NOT NULL,
    status        VARCHAR(10)   NOT NULL CHECK (status IN ('disponivel','retido','estornado','solicitado','pago')),
    criado_em     TIMESTAMPTZ   NOT NULL DEFAULT now()
);
CREATE INDEX ix_movimentos_motorista ON movimentos (motorista_id, criado_em DESC);
CREATE UNIQUE INDEX ux_movimento_frete_lote ON movimentos (lote_id) WHERE tipo = 'frete';

-- ---------- Faturas das empresas ----------
CREATE TABLE faturas (
    id          BIGSERIAL PRIMARY KEY,
    empresa_id  BIGINT        NOT NULL REFERENCES usuarios(id) ON DELETE CASCADE,
    referencia  DATE          NOT NULL,          -- sempre o dia 1 do mês
    plano       VARCHAR(10)   NOT NULL,
    valor       NUMERIC(12,2) NOT NULL,
    extras      NUMERIC(12,2) NOT NULL DEFAULT 0,
    status      VARCHAR(8)    NOT NULL DEFAULT 'aberta' CHECK (status IN ('aberta','paga','atrasada')),
    criado_em   TIMESTAMPTZ   NOT NULL DEFAULT now(),
    UNIQUE (empresa_id, referencia)
);

-- ---------- Ocorrências, notificações e auditoria ----------
CREATE TABLE ocorrencias (
    id            BIGSERIAL PRIMARY KEY,
    lote_id       BIGINT       REFERENCES lotes(id) ON DELETE SET NULL,
    tipo          VARCHAR(60)  NOT NULL,
    status        VARCHAR(10)  NOT NULL DEFAULT 'aberta' CHECK (status IN ('aberta','analise','resolvida')),
    obs           VARCHAR(600),
    resolucao     VARCHAR(400),
    autor_id      BIGINT       REFERENCES usuarios(id) ON DELETE SET NULL,
    criado_em     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    resolvido_em  TIMESTAMPTZ
);
CREATE INDEX ix_ocorrencias_status ON ocorrencias (status, criado_em DESC);

CREATE TABLE notificacoes (
    id          BIGSERIAL PRIMARY KEY,
    usuario_id  BIGINT       NOT NULL REFERENCES usuarios(id) ON DELETE CASCADE,
    texto       VARCHAR(300) NOT NULL,
    icone       VARCHAR(40)  NOT NULL DEFAULT 'fa-bell',
    lida        BOOLEAN      NOT NULL DEFAULT FALSE,
    criado_em   TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE INDEX ix_notificacoes_usuario ON notificacoes (usuario_id, criado_em DESC);

CREATE TABLE auditoria (
    id          BIGSERIAL PRIMARY KEY,
    ator_id     BIGINT       REFERENCES usuarios(id) ON DELETE SET NULL,
    ator_nome   VARCHAR(160) NOT NULL,
    acao        VARCHAR(120) NOT NULL,
    alvo        VARCHAR(160) NOT NULL,
    alvo_id     BIGINT,
    extra       VARCHAR(600),
    criado_em   TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE INDEX ix_auditoria_data ON auditoria (criado_em DESC);
CREATE INDEX ix_auditoria_alvo ON auditoria (alvo_id);
