-- OngSave · V003 · Marca os lotes criados pelo modo simulação (app.simulacao=true).
-- Os motoristas automáticos só pegam lotes simulados; os publicados à mão ficam para o apresentador.
ALTER TABLE lotes ADD COLUMN IF NOT EXISTS simulado BOOLEAN NOT NULL DEFAULT FALSE;
CREATE INDEX IF NOT EXISTS ix_lotes_simulado ON lotes (estado) WHERE simulado;
