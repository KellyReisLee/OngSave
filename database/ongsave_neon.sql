-- =====================================================================
--  OngSave · Banco de dados completo para o Neon (PostgreSQL 15+)
--  Como usar: Neon > SQL Editor > cole TUDO isto > Run.
--  Use num banco VAZIO (ex.: neondb recém-criado).
--
--  Conteúdo: V001 (esquema) + V002 (configuração, admin e dados demo)
--            + V003 (coluna "simulado") + V004 (tokens de recuperação de senha)
--            + registo em schema_versao,
--  para que a aplicação reconheça o banco como já preparado e NÃO tente
--  criar as tabelas de novo no arranque.
--
--  Contas de demonstração: senha ongsave123
--    admin@ongsave.com · empresa@ongsave.com · motorista@ongsave.com · ong@ongsave.com
--  Quer só o admin, sem dados demo? Apague o bloco entre
--  "INÍCIO DADOS DEMO" e "FIM DADOS DEMO".
-- =====================================================================

-- (Opcional) Para recomeçar do zero num banco que já tem as tabelas, descomente:
-- DROP SCHEMA public CASCADE; CREATE SCHEMA public;

BEGIN;

SET client_encoding = 'UTF8';

-- ---------- Controlo de versões (lido pelo Migrador da aplicação) ----------
CREATE TABLE schema_versao (
    versao      INT PRIMARY KEY,
    descricao   VARCHAR(200) NOT NULL,
    aplicada_em TIMESTAMPTZ  NOT NULL DEFAULT now()
);

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

-- ---------- V004 · Tokens de recuperação de senha ----------
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

-- ---------- V003 · Marca dos lotes criados pelo modo simulação ----------
ALTER TABLE lotes ADD COLUMN IF NOT EXISTS simulado BOOLEAN NOT NULL DEFAULT FALSE;
CREATE INDEX IF NOT EXISTS ix_lotes_simulado ON lotes (estado) WHERE simulado;

-- =====================================================================
-- V002 · Parâmetros da plataforma + administrador
-- =====================================================================
INSERT INTO plataforma_config (id, dados) VALUES (1, '{
  "raioKm": 15,
  "fretes": {"carro": 25, "camionete": 45, "van": 70, "caminhao": 110},
  "reservaPct": 25,
  "planos": {
    "pequena": {"nome": "Pequena Empresa", "ex": "Padarias e hortifrutis locais", "preco": 500, "franquia": 6, "taxaExtra": 55, "ret": "Selo local de estabelecimento sustentável"},
    "media":   {"nome": "Média Empresa", "ex": "Supermercados regionais e distribuidores", "preco": 2000, "franquia": 20, "taxaExtra": 70, "ret": "Relatórios mensais de impacto para auditorias internas"},
    "grande":  {"nome": "Grande Empresa", "ex": "Hipermercados, redes de varejo e indústrias", "preco": 8000, "franquia": 60, "taxaExtra": 90, "ret": "Painel corporativo em tempo real e conformidade ESG"}
  },
  "metodologia": {"co2PorKg": 1.69, "refeicoesPorKg": 2, "fonte": ""},
  "regras": {"rejeicaoPct": 15, "minEntregas": 10, "tolPesoPct": 5, "concentracaoPct": 60}
}'::jsonb);

-- Administrador inicial (sempre criado). TROQUE A SENHA após o primeiro login em produção.
INSERT INTO usuarios (perfil, nome, email, senha_hash, status, documento, telefone)
VALUES ('admin', 'Carlos Alberto', 'admin@ongsave.com',
        'pbkdf2_sha256$120000$T25nU2F2ZURlbW9TYWwwMQ==$DQpoBUERcitaY2Ji8o8QK8AZxi7Wng+u5r/fFP3cBqY=',
        'ativo', NULL, '(11) 90000-0000');


-- ======================= INÍCIO DADOS DEMO =======================
-- ---------- Empresas ----------
WITH novos AS (
  INSERT INTO usuarios (perfil, nome, email, senha_hash, status, documento, telefone, criado_em)
  VALUES
   ('empresa','Supermercado Bom Preço','empresa@ongsave.com','pbkdf2_sha256$120000$T25nU2F2ZURlbW9TYWwwMQ==$DQpoBUERcitaY2Ji8o8QK8AZxi7Wng+u5r/fFP3cBqY=','ativo','12.345.678/0001-90','(11) 98765-4321', now() - interval '210 days'),
   ('empresa','Padaria Pão Dourado','padaria@exemplo.com.br','pbkdf2_sha256$120000$T25nU2F2ZURlbW9TYWwwMQ==$DQpoBUERcitaY2Ji8o8QK8AZxi7Wng+u5r/fFP3cBqY=','ativo','23.456.789/0001-01','(11) 94410-1311', now() - interval '160 days'),
   ('empresa','Hortifruti Verde Vivo','verdevivo@exemplo.com.br','pbkdf2_sha256$120000$T25nU2F2ZURlbW9TYWwwMQ==$DQpoBUERcitaY2Ji8o8QK8AZxi7Wng+u5r/fFP3cBqY=','ativo','34.567.890/0001-12','(11) 94547-1622', now() - interval '140 days'),
   ('empresa','Atacadão Central','atacadao@exemplo.com.br','pbkdf2_sha256$120000$T25nU2F2ZURlbW9TYWwwMQ==$DQpoBUERcitaY2Ji8o8QK8AZxi7Wng+u5r/fFP3cBqY=','ativo','45.678.901/0001-23','(11) 94684-1933', now() - interval '120 days'),
   ('empresa','Rede Fresh Market','fresh@exemplo.com.br','pbkdf2_sha256$120000$T25nU2F2ZURlbW9TYWwwMQ==$DQpoBUERcitaY2Ji8o8QK8AZxi7Wng+u5r/fFP3cBqY=','pendente','67.890.123/0001-45','(11) 94821-2244', now() - interval '1 day')
  RETURNING id, email
)
INSERT INTO empresas (usuario_id, setor, responsavel, plano, custo_descarte, cep, rua, numero, complemento, bairro, cidade, uf, lat, lon)
SELECT n.id, d.setor, d.resp, d.plano, d.descarte, d.cep, d.rua, d.num, d.compl, d.bairro, 'São Paulo', 'SP', d.lat, d.lon
FROM novos n JOIN (VALUES
  ('empresa@ongsave.com','Supermercado','Roberto Lima','media',3200.00,'01310-100','Av. Paulista','1200','Loja B','Bela Vista',-23.5614,-46.6560),
  ('padaria@exemplo.com.br','Padaria','Helena Prado','pequena',NULL,'04110-000','Rua Domingos de Morais','800',NULL,'Vila Mariana',-23.6000,-46.6200),
  ('verdevivo@exemplo.com.br','Hortifrúti','Marcos Vieira','pequena',900.00,'05402-000','Rua Teodoro Sampaio','450',NULL,'Pinheiros',-23.5700,-46.6800),
  ('atacadao@exemplo.com.br','Atacado','Sônia Barros','grande',12000.00,'03012-000','Rua do Gasômetro','300',NULL,'Brás',-23.5400,-46.6100),
  ('fresh@exemplo.com.br','Supermercado','Paula Reis','grande',NULL,'04571-000','Av. Engenheiro Luís Carlos Berrini','1500',NULL,'Brooklin',-23.6020,-46.6930)
) AS d(email, setor, resp, plano, descarte, cep, rua, num, compl, bairro, lat, lon) ON d.email = n.email;

-- ---------- Motoristas ----------
WITH novos AS (
  INSERT INTO usuarios (perfil, nome, email, senha_hash, status, documento, telefone, criado_em)
  VALUES
   ('motorista','João da Silva','motorista@ongsave.com','pbkdf2_sha256$120000$T25nU2F2ZURlbW9TYWwwMQ==$DQpoBUERcitaY2Ji8o8QK8AZxi7Wng+u5r/fFP3cBqY=','ativo','123.456.789-00','(11) 91234-5678', now() - interval '190 days'),
   ('motorista','Marina Souza','marina@email.com','pbkdf2_sha256$120000$T25nU2F2ZURlbW9TYWwwMQ==$DQpoBUERcitaY2Ji8o8QK8AZxi7Wng+u5r/fFP3cBqY=','ativo','222.333.444-55','(11) 94959-2555', now() - interval '150 days'),
   ('motorista','Paulo Ribeiro','paulo@email.com','pbkdf2_sha256$120000$T25nU2F2ZURlbW9TYWwwMQ==$DQpoBUERcitaY2Ji8o8QK8AZxi7Wng+u5r/fFP3cBqY=','ativo','333.444.555-66','(11) 95096-2866', now() - interval '130 days'),
   ('motorista','Lucas Ferraz','lucas@email.com','pbkdf2_sha256$120000$T25nU2F2ZURlbW9TYWwwMQ==$DQpoBUERcitaY2Ji8o8QK8AZxi7Wng+u5r/fFP3cBqY=','ativo','666.777.888-99','(11) 95233-3177', now() - interval '60 days'),
   ('motorista','Rafael Teixeira','rafael@email.com','pbkdf2_sha256$120000$T25nU2F2ZURlbW9TYWwwMQ==$DQpoBUERcitaY2Ji8o8QK8AZxi7Wng+u5r/fFP3cBqY=','pendente','999.000.111-22','(11) 95370-3488', now() - interval '2 days')
  RETURNING id, email
)
INSERT INTO motoristas (usuario_id, veiculo, placa, modelo, cnh, cnh_categoria, cnh_validade, cep, rua, numero, bairro, cidade, uf)
SELECT n.id, d.veic, d.placa, d.modelo, d.cnh, d.cat, (current_date + d.dias), '04101-000', 'Rua das Palmeiras', '245', 'Vila Mariana', 'São Paulo', 'SP'
FROM novos n JOIN (VALUES
  ('motorista@ongsave.com','Van','ABC1D23','Renault Master 2020','00123456789','B',400),
  ('marina@email.com','Caminhão','QWE4R56','VW Delivery 2019','00234567891','C',24),
  ('paulo@email.com','Carro Económico','JKL8M90','Fiat Uno 2018','00345678912','B',210),
  ('lucas@email.com','Van','POI9U87','Fiat Ducato 2021','00456789123','B',180),
  ('rafael@email.com','Van','GHJ6T54','Iveco Daily 2022','00567891234','B',700)
) AS d(email, veic, placa, modelo, cnh, cat, dias) ON d.email = n.email;

-- ---------- ONGs ----------
WITH novos AS (
  INSERT INTO usuarios (perfil, nome, email, senha_hash, status, documento, telefone, criado_em)
  VALUES
   ('ong','Casa Esperança','ong@ongsave.com','pbkdf2_sha256$120000$T25nU2F2ZURlbW9TYWwwMQ==$DQpoBUERcitaY2Ji8o8QK8AZxi7Wng+u5r/fFP3cBqY=','ativo','98.765.432/0001-10','(11) 3456-7890', now() - interval '200 days'),
   ('ong','Mãos Solidárias','maos@exemplo.org.br','pbkdf2_sha256$120000$T25nU2F2ZURlbW9TYWwwMQ==$DQpoBUERcitaY2Ji8o8QK8AZxi7Wng+u5r/fFP3cBqY=','ativo','87.654.321/0001-20','(11) 3567-8901', now() - interval '170 days'),
   ('ong','Prato Cheio','pratocheio@exemplo.org.br','pbkdf2_sha256$120000$T25nU2F2ZURlbW9TYWwwMQ==$DQpoBUERcitaY2Ji8o8QK8AZxi7Wng+u5r/fFP3cBqY=','ativo','76.543.210/0001-30','(11) 3678-9012', now() - interval '100 days'),
   ('ong','Banco de Alimentos Regional','banco@exemplo.org.br','pbkdf2_sha256$120000$T25nU2F2ZURlbW9TYWwwMQ==$DQpoBUERcitaY2Ji8o8QK8AZxi7Wng+u5r/fFP3cBqY=','ativo','65.432.109/0001-40','(11) 3789-0123', now() - interval '220 days'),
   ('ong','Instituto Semear','semear@exemplo.org.br','pbkdf2_sha256$120000$T25nU2F2ZURlbW9TYWwwMQ==$DQpoBUERcitaY2Ji8o8QK8AZxi7Wng+u5r/fFP3cBqY=','pendente','54.321.098/0001-50','(11) 3890-1234', now() - interval '4 days')
  RETURNING id, email
)
INSERT INTO ongs (usuario_id, razao_social, responsavel, familias, capacidade_kg, camara_fria, alvara_validade, cep, rua, numero, bairro, cidade, uf, lat, lon)
SELECT n.id, d.razao, d.resp, d.fam, d.cap, d.frio, (current_date + d.alv), d.cep, d.rua, d.num, d.bairro, 'São Paulo', 'SP', d.lat, d.lon
FROM novos n JOIN (VALUES
  ('ong@ongsave.com','Associação Comunitária Esperança','Maria Oliveira',150,600,TRUE,45,'04101-000','Rua da Esperança','88','Vila Mariana',-23.5880,-46.6390),
  ('maos@exemplo.org.br','Instituto Mãos Solidárias','José Ramos',220,900,TRUE,300,'01227-000','Rua Sergipe','120','Higienópolis',-23.5300,-46.6700),
  ('pratocheio@exemplo.org.br','Associação Prato Cheio','Luíza Andrade',90,300,FALSE,20,'03103-000','Rua Tabatinguera','60','Cambuci',-23.5700,-46.6100),
  ('banco@exemplo.org.br','Banco de Alimentos Regional','Antônio Reis',600,2500,TRUE,500,'01504-000','Rua da Glória','400','Liberdade',-23.5450,-46.6200),
  ('semear@exemplo.org.br','Instituto Semear','Clara Nunes',70,200,FALSE,365,'02011-000','Rua Voluntários da Pátria','900','Santana',-23.5040,-46.6270)
) AS d(email, razao, resp, fam, cap, frio, alv, cep, rua, num, bairro, lat, lon) ON d.email = n.email;

-- ---------- Documentos de cadastro ----------
INSERT INTO documentos (usuario_id, nome, arquivo_nome, status)
SELECT u.id, d.nome, lower(split_part(d.nome, ' ', 1)) || '-' || u.id || '.pdf',
       CASE WHEN u.status = 'pendente' AND d.ordem % 2 = 0 THEN 'analise' ELSE 'ok' END
FROM usuarios u
JOIN (VALUES
  ('empresa', 1, 'Contrato social e CNPJ'), ('empresa', 2, 'Termo de responsabilidade técnica'), ('empresa', 3, 'Comprovante de endereço'),
  ('motorista', 1, 'CNH'), ('motorista', 2, 'CRLV do veículo'), ('motorista', 3, 'Comprovante de residência'), ('motorista', 4, 'Foto do veículo'),
  ('ong', 1, 'Alvará / registro da instituição'), ('ong', 2, 'Estatuto e ata da diretoria'), ('ong', 3, 'Comprovante de endereço'), ('ong', 4, 'Documento do responsável')
) AS d(perfil, ordem, nome) ON d.perfil = u.perfil;

-- ---------- Histórico: ~170 lotes entregues nos últimos 6 meses ----------
ALTER SEQUENCE lotes_id_seq RESTART WITH 8700;

WITH e AS (SELECT array_agg(id ORDER BY id) a FROM usuarios WHERE perfil = 'empresa' AND status = 'ativo'),
     o AS (SELECT array_agg(id ORDER BY id) a FROM usuarios WHERE perfil = 'ong' AND status = 'ativo'),
     m AS (SELECT array_agg(id ORDER BY id) a FROM usuarios WHERE perfil = 'motorista' AND status = 'ativo'),
     base AS (
       SELECT g,
              (20 + (g * 37) % 360)::numeric AS peso,
              (ARRAY['Laticínios e Frios','Hortifrúti / Frutas e Verduras','Padaria e Panificados','Mercearia / Não Perecíveis','Pratos Prontos / Marmitas'])[1 + g % 5] AS cat,
              now() - make_interval(hours => g * 25 + 30) AS criado
       FROM generate_series(1, 170) g
     )
INSERT INTO lotes (empresa_id, ong_id, motorista_id, categoria, peso_kg, volumes, conservacao, veiculo, validade, estado, frete,
                   criado_em, ong_aceite_em, aceito_em, retirada_codigo, retirada_kg, retirada_em, coleta_em,
                   token_codigo, token_gerado_em, token_expira_em, token_usado_em, entregue_em, entrega_dist_m,
                   conf_kg, conf_condicao, conf_div_pct, conf_em)
SELECT e.a[1 + b.g % array_length(e.a, 1)],
       o.a[1 + (b.g * 7) % array_length(o.a, 1)],
       m.a[1 + (b.g * 3) % array_length(m.a, 1)],
       b.cat, b.peso, 1 + b.g % 8,
       CASE WHEN b.cat = 'Laticínios e Frios' THEN 'ref' ELSE 'amb' END,
       CASE WHEN b.peso > 300 THEN 'cam' WHEN b.peso > 80 THEN 'van' ELSE 'carro' END,
       b.criado + interval '30 hours', 'entregue',
       CASE WHEN b.peso > 300 THEN 110 WHEN b.peso > 80 THEN 70 ELSE 25 END,
       b.criado, b.criado + interval '10 minutes', b.criado + interval '25 minutes',
       'DEMO' || lpad((b.g % 100)::text, 2, '0'), b.peso, b.criado + interval '50 minutes', b.criado + interval '55 minutes',
       'TK' || lpad((b.g % 10000)::text, 4, '0'), b.criado + interval '100 minutes', b.criado + interval '110 minutes',
       b.criado + interval '103 minutes', b.criado + interval '103 minutes', 10 + (b.g * 13) % 60,
       b.peso, 'ok', 0, b.criado + interval '2 hours'
FROM base b, e, o, m;

-- Fretes do histórico na carteira dos motoristas (liberados após a conferência)
INSERT INTO movimentos (motorista_id, lote_id, tipo, descricao, valor, status, criado_em)
SELECT l.motorista_id, l.id, 'frete',
       'Frete #' || l.id || ' · ' || ue.nome || ' → ' || uo.nome, l.frete, 'disponivel', l.conf_em
FROM lotes l
JOIN usuarios ue ON ue.id = l.empresa_id
JOIN usuarios uo ON uo.id = l.ong_id
WHERE l.estado = 'entregue';

-- Saques mensais já pagos (80% do que entrou em cada mês fechado)
INSERT INTO movimentos (motorista_id, tipo, descricao, valor, status, criado_em)
SELECT motorista_id, 'saque', 'Saque via Pix', -round(sum(valor) * 0.8, 2), 'pago',
       date_trunc('month', criado_em) + interval '1 month' + interval '2 days'
FROM movimentos
WHERE tipo = 'frete' AND criado_em < date_trunc('month', now())
GROUP BY motorista_id, date_trunc('month', criado_em);

-- Faturas pagas dos últimos 5 meses das empresas ativas
INSERT INTO faturas (empresa_id, referencia, plano, valor, extras, status, criado_em)
SELECT emp.usuario_id, (date_trunc('month', now()) - make_interval(months => k))::date, emp.plano,
       (cfg.dados #>> ARRAY['planos', emp.plano::text, 'preco'])::numeric, 0, 'paga',
       date_trunc('month', now()) - make_interval(months => k) + interval '14 days'
FROM empresas emp
JOIN usuarios u ON u.id = emp.usuario_id AND u.status = 'ativo'
CROSS JOIN generate_series(1, 5) k
CROSS JOIN plataforma_config cfg;

-- ---------- Operação ao vivo (para explorar os painéis) ----------
-- 1) Lote do Supermercado Bom Preço já aceite pela Casa Esperança: aparece para os motoristas.
INSERT INTO lotes (empresa_id, ong_id, categoria, peso_kg, volumes, conservacao, veiculo, validade, estado, criado_em, ong_aceite_em, observacao)
SELECT (SELECT id FROM usuarios WHERE email = 'empresa@ongsave.com'),
       (SELECT id FROM usuarios WHERE email = 'ong@ongsave.com'),
       'Hortifrúti / Frutas e Verduras', 60, 6, 'amb', 'van', now() + interval '20 hours', 'aguardando',
       now() - interval '40 minutes', now() - interval '30 minutes', 'Caixas no dock 2, portão lateral.';

-- 2) Proposta ainda não aceite: aparece em "Doações disponíveis" da Casa Esperança.
INSERT INTO lotes (empresa_id, ong_id, categoria, peso_kg, volumes, conservacao, veiculo, validade, estado, criado_em)
SELECT (SELECT id FROM usuarios WHERE email = 'atacadao@exemplo.com.br'),
       (SELECT id FROM usuarios WHERE email = 'ong@ongsave.com'),
       'Mercearia / Não Perecíveis', 180, 12, 'amb', 'van', now() + interval '40 hours', 'aguardando', now() - interval '15 minutes';

-- 3) Entrega em trânsito (Marina) do Hortifruti Verde Vivo para a Casa Esperança.
INSERT INTO lotes (empresa_id, ong_id, motorista_id, categoria, peso_kg, volumes, conservacao, veiculo, validade, estado, frete,
                   criado_em, ong_aceite_em, aceito_em, retirada_codigo, retirada_kg, retirada_em, coleta_em)
SELECT (SELECT id FROM usuarios WHERE email = 'verdevivo@exemplo.com.br'),
       (SELECT id FROM usuarios WHERE email = 'ong@ongsave.com'),
       (SELECT id FROM usuarios WHERE email = 'marina@email.com'),
       'Hortifrúti / Frutas e Verduras', 120, 10, 'amb', 'van', now() + interval '9 hours', 'transito', 70,
       now() - interval '2 hours', now() - interval '110 minutes', now() - interval '90 minutes',
       'VV7K2M', 118, now() - interval '50 minutes', now() - interval '45 minutes';

INSERT INTO posicoes (lote_id, motorista_id, lat, lon, precisao_m, registado_em)
SELECT l.id, l.motorista_id, -23.5700 + (-23.5880 + 23.5700) * p.f, -46.6800 + (-46.6390 + 46.6800) * p.f, 12, now() - make_interval(mins => (10 - p.i) * 4)
FROM lotes l
CROSS JOIN (SELECT i, i / 10.0 * 0.6 AS f FROM generate_series(1, 10) i) p
WHERE l.estado = 'transito';

-- 4) Ocorrência aberta num lote recente (frete retido até a análise)
UPDATE lotes SET conf_kg = peso_kg * 0.8, conf_condicao = 'parcial', conf_div_pct = 20,
                 conf_obs = 'Parte das frutas chegou machucada e foi descartada.', frete_retido = TRUE
WHERE id = (SELECT min(id) FROM lotes WHERE estado = 'entregue');

UPDATE movimentos SET status = 'retido'
WHERE lote_id = (SELECT min(id) FROM lotes WHERE estado = 'entregue');

INSERT INTO ocorrencias (lote_id, tipo, status, obs, autor_id, criado_em)
SELECT l.id, 'Alimento impróprio', 'aberta',
       'Condição informada: parcialmente impróprio. Peso na retirada ' || l.peso_kg || ' kg, recebido ' || l.conf_kg || ' kg (20%).',
       l.ong_id, now() - interval '1 day'
FROM lotes l WHERE l.id = (SELECT min(id) FROM lotes WHERE estado = 'entregue');

-- ---------- Notificações e auditoria iniciais ----------
INSERT INTO notificacoes (usuario_id, texto, icone, lida, criado_em)
SELECT id, 'Bem-vindo à OngSave! Os dados deste painel vêm do banco de dados.', 'fa-circle-check', FALSE, now()
FROM usuarios WHERE status = 'ativo' AND perfil <> 'motorista';

INSERT INTO auditoria (ator_nome, acao, alvo, extra)
VALUES ('Sistema', 'Instalou a plataforma', 'Plataforma', 'Banco criado e dados de demonstração carregados');
-- ======================== FIM DADOS DEMO =========================

-- ---------- Regista as migrações como aplicadas ----------
INSERT INTO schema_versao (versao, descricao) VALUES
  (1, 'esquema'),
  (2, 'dados iniciais'),
  (3, 'simulacao'),
  (4, 'recuperacao senha por email');

COMMIT;

-- Verificação rápida (opcional):
-- SELECT perfil, status, count(*) FROM usuarios GROUP BY 1,2 ORDER BY 1,2;
-- SELECT estado, count(*) FROM lotes GROUP BY 1;
