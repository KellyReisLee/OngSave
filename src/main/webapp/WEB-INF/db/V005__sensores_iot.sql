-- OngSave · V005 · Sensores IoT (simulados): temperatura da câmara fria da ONG e balança de carga da empresa.
-- Cada leitura é gravada; quando sai do limite, a plataforma marca o alerta e avisa o dono do sensor.
CREATE TABLE IF NOT EXISTS sensores (
    id          VARCHAR(60)   PRIMARY KEY,                       -- ex.: camara-fria-casa-esperanca
    tipo        VARCHAR(20)   NOT NULL CHECK (tipo IN ('temperatura', 'peso')),
    usuario_id  BIGINT        NOT NULL REFERENCES usuarios(id) ON DELETE CASCADE,  -- ONG ou empresa dona
    descricao   VARCHAR(120)  NOT NULL,
    unidade     VARCHAR(10)   NOT NULL,
    limite_min  NUMERIC(8,2),                                    -- temperatura: faixa segura
    limite_max  NUMERIC(8,2),                                    -- peso: capacidade da balança
    ativo       BOOLEAN       NOT NULL DEFAULT TRUE,
    criado_em   TIMESTAMPTZ   NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS leituras_sensores (
    id           BIGSERIAL     PRIMARY KEY,
    sensor_id    VARCHAR(60)   NOT NULL REFERENCES sensores(id) ON DELETE CASCADE,
    lote_id      BIGINT        REFERENCES lotes(id) ON DELETE SET NULL,   -- balança: lote pesado
    valor        NUMERIC(10,2) NOT NULL,
    alerta       BOOLEAN       NOT NULL DEFAULT FALSE,
    motivo       VARCHAR(200),
    registada_em TIMESTAMPTZ   NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS ix_leituras_sensor ON leituras_sensores (sensor_id, registada_em DESC);

-- Sensores das contas de demonstração (só se as contas existirem).
INSERT INTO sensores (id, tipo, usuario_id, descricao, unidade, limite_min, limite_max)
SELECT 'camara-fria-casa-esperanca', 'temperatura', u.id, 'Câmara fria da Casa Esperança', '°C', 2, 8
FROM usuarios u WHERE lower(u.email) = 'ong@ongsave.com'
ON CONFLICT (id) DO NOTHING;

INSERT INTO sensores (id, tipo, usuario_id, descricao, unidade, limite_min, limite_max)
SELECT 'balanca-doca-bom-preco', 'peso', u.id, 'Balança da doca de carga do Supermercado Bom Preço', 'kg', 0, 1000
FROM usuarios u WHERE lower(u.email) = 'empresa@ongsave.com'
ON CONFLICT (id) DO NOTHING;
