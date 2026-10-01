-- =====================================================================
-- OngSave · V002 · Parâmetros da plataforma + dados de demonstração
-- Todas as contas de demonstração usam a senha: ongsave123
-- Para produção sem dados de exemplo, defina app.dadosDemo=false em
-- ongsave.properties ANTES do primeiro arranque (só os parâmetros e o
-- administrador são criados; ver Migrador).
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

-- --------------------------------------------------------------------
-- A partir daqui: dados de demonstração (removidos pelo Migrador se
-- app.dadosDemo=false). Marcador lido pelo Migrador:
-- @@DEMO@@
-- --------------------------------------------------------------------

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
