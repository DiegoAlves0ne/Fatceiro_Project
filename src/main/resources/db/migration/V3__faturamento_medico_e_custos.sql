ALTER TABLE titulo ADD COLUMN medico_id UUID;
ALTER TABLE titulo ADD COLUMN data_competencia DATE;
ALTER TABLE titulo ADD COLUMN categoria VARCHAR(20);
ALTER TABLE titulo ADD COLUMN custos_informados BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE titulo ADD CONSTRAINT ck_titulo_medico CHECK (
 medico_id IS NULL OR (tipo = 'RECEBER' AND atendimento_id IS NOT NULL AND data_competencia IS NOT NULL
 AND categoria IS NOT NULL AND categoria IN ('CONSULTA','EXAME'))
);
-- Um faturamento por atendimento; registros anteriores sem médico são preservados.
CREATE UNIQUE INDEX uk_faturamento_atendimento ON titulo(atendimento_id) WHERE medico_id IS NOT NULL;
CREATE INDEX idx_titulo_medico_competencia ON titulo(medico_id,data_competencia);
CREATE INDEX idx_pagamento_data ON pagamento(criado_em,titulo_id);
CREATE TABLE custo_item (
 id UUID PRIMARY KEY, titulo_id UUID NOT NULL REFERENCES titulo(id),
 item_estoque_id UUID NOT NULL, descricao VARCHAR(200) NOT NULL,
 quantidade NUMERIC(12,3) NOT NULL CHECK (quantidade > 0),
 custo_unitario NUMERIC(19,2) NOT NULL CHECK (custo_unitario >= 0),
 custo_total NUMERIC(19,2) NOT NULL CHECK (custo_total >= 0),
 UNIQUE(titulo_id,item_estoque_id)
);
