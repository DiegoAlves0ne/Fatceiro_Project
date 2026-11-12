CREATE TABLE titulo (
 id UUID PRIMARY KEY, version BIGINT NOT NULL DEFAULT 0,
 tipo VARCHAR(255) NOT NULL CHECK (tipo IN ('RECEBER','PAGAR')),
 status VARCHAR(255) NOT NULL CHECK (status IN ('ABERTO','PARCIAL','QUITADO','CANCELADO')),
 descricao VARCHAR(200) NOT NULL, paciente_id UUID, atendimento_id UUID,
 valor NUMERIC(19,2) NOT NULL CHECK (valor > 0),
 liquidado NUMERIC(19,2) NOT NULL CHECK (liquidado >= 0 AND liquidado <= valor),
 vencimento DATE NOT NULL
);
CREATE INDEX idx_titulo_vencimento ON titulo(vencimento,id);
CREATE INDEX idx_titulo_atendimento ON titulo(atendimento_id);
CREATE TABLE pagamento (
 id UUID PRIMARY KEY, titulo_id UUID NOT NULL REFERENCES titulo(id),
 chave_idempotencia VARCHAR(100) NOT NULL UNIQUE,
 valor NUMERIC(19,2) NOT NULL CHECK (valor > 0), criado_em TIMESTAMP WITH TIME ZONE NOT NULL
);
CREATE INDEX idx_pagamento_titulo ON pagamento(titulo_id);
