-- Modelo do diagrama base; tabelas e dados legados permanecem preservados.

CREATE TABLE conta_receber (
    id UUID PRIMARY KEY,
    version BIGINT NOT NULL DEFAULT 0,
    tipo_devedor VARCHAR(30) NOT NULL CHECK (tipo_devedor IN ('PACIENTE', 'CONVENIO')),
    devedor_id UUID NOT NULL,
    data_emissao DATE NOT NULL,
    data_vencimento DATE NOT NULL,
    valor_original NUMERIC(19,2) NOT NULL CHECK (valor_original > 0),
    status VARCHAR(30) NOT NULL CHECK (status IN ('ABERTO', 'PARCIAL', 'QUITADO', 'CANCELADO'))
);


CREATE TABLE conta_pagar (
    id UUID PRIMARY KEY,
    version BIGINT NOT NULL DEFAULT 0,
    tipo_credor VARCHAR(30) NOT NULL CHECK (tipo_credor IN ('FORNECEDOR', 'MEDICO')),
    credor_id UUID NOT NULL,
    data_emissao DATE NOT NULL,
    data_vencimento DATE NOT NULL,
    valor_original NUMERIC(19,2) NOT NULL CHECK (valor_original > 0),
    status VARCHAR(30) NOT NULL CHECK (status IN ('ABERTO', 'PARCIAL', 'QUITADO', 'CANCELADO'))
);


CREATE TABLE recebimento (
    id UUID PRIMARY KEY,
    version BIGINT NOT NULL DEFAULT 0,
    data_hora TIMESTAMP WITH TIME ZONE NOT NULL,
    valor NUMERIC(19,2) NOT NULL CHECK (valor > 0),
    forma VARCHAR(30) NOT NULL CHECK (forma IN ('PIX', 'DINHEIRO', 'CARTAO_CREDITO', 'CARTAO_DEBITO', 'BOLETO', 'TRANSFERENCIA', 'NAO_INFORMADA')),
    referencia_transacao VARCHAR(200)
);


CREATE TABLE pagamento_conta (
    id UUID PRIMARY KEY,
    version BIGINT NOT NULL DEFAULT 0,
    data_hora TIMESTAMP WITH TIME ZONE NOT NULL,
    valor NUMERIC(19,2) NOT NULL CHECK (valor > 0),
    forma VARCHAR(30) NOT NULL CHECK (forma IN ('PIX', 'DINHEIRO', 'CARTAO_CREDITO', 'CARTAO_DEBITO', 'BOLETO', 'TRANSFERENCIA', 'NAO_INFORMADA')),
    referencia_transacao VARCHAR(200)
);


CREATE TABLE item_conta_receber (
    id UUID PRIMARY KEY,
    conta_receber_id UUID NOT NULL REFERENCES conta_receber(id),
    item_agendamento_id UUID NOT NULL,
    atendimento_id UUID NOT NULL,
    descricao VARCHAR(200) NOT NULL,
    valor NUMERIC(19,2) NOT NULL CHECK (valor > 0)
);
CREATE INDEX idx_item_conta_receber_conta_receber_id ON item_conta_receber(conta_receber_id);

CREATE TABLE item_conta_pagar (
    id UUID PRIMARY KEY,
    conta_pagar_id UUID NOT NULL REFERENCES conta_pagar(id),
    origem VARCHAR(30) NOT NULL CHECK (origem IN ('COMPRA_ESTOQUE', 'REPASSE_MEDICO', 'DESPESA')),
    origem_id UUID NOT NULL,
    descricao VARCHAR(200) NOT NULL,
    valor NUMERIC(19,2) NOT NULL CHECK (valor > 0)
);
CREATE INDEX idx_item_conta_pagar_conta_pagar_id ON item_conta_pagar(conta_pagar_id);

CREATE TABLE aplicacao_recebimento (
    id UUID PRIMARY KEY,
    conta_receber_id UUID NOT NULL REFERENCES conta_receber(id),
    recebimento_id UUID NOT NULL REFERENCES recebimento(id),
    valor_aplicado NUMERIC(19,2) NOT NULL CHECK (valor_aplicado > 0)
);
CREATE INDEX idx_aplicacao_recebimento_conta_receber_id ON aplicacao_recebimento(conta_receber_id);
CREATE INDEX idx_aplicacao_recebimento_recebimento_id ON aplicacao_recebimento(recebimento_id);

CREATE TABLE aplicacao_pagamento (
    id UUID PRIMARY KEY,
    conta_pagar_id UUID NOT NULL REFERENCES conta_pagar(id),
    pagamento_id UUID NOT NULL REFERENCES pagamento_conta(id),
    valor_aplicado NUMERIC(19,2) NOT NULL CHECK (valor_aplicado > 0)
);
CREATE INDEX idx_aplicacao_pagamento_conta_pagar_id ON aplicacao_pagamento(conta_pagar_id);
CREATE INDEX idx_aplicacao_pagamento_pagamento_id ON aplicacao_pagamento(pagamento_id);

CREATE TABLE fatura_convenio (
    id UUID PRIMARY KEY,
    version BIGINT NOT NULL DEFAULT 0,
    convenio_id UUID NOT NULL,
    competencia DATE NOT NULL,
    data_emissao DATE NOT NULL,
    status VARCHAR(30) NOT NULL CHECK (status IN ('ABERTA', 'ENVIADA', 'PROCESSADA', 'CANCELADA'))
);


CREATE TABLE item_fatura_convenio (
    id UUID PRIMARY KEY,
    fatura_convenio_id UUID NOT NULL REFERENCES fatura_convenio(id),
    item_conta_receber_id UUID NOT NULL UNIQUE REFERENCES item_conta_receber(id),
    atendimento_id UUID NOT NULL,
    numero_autorizacao VARCHAR(255),
    valor_apresentado NUMERIC(19,2) NOT NULL CHECK (valor_apresentado > 0),
    valor_glosado NUMERIC(19,2) NOT NULL CHECK (valor_glosado >= 0),
    motivo_glosa VARCHAR(255),
    CHECK (valor_glosado <= valor_apresentado)
);
CREATE INDEX idx_item_fatura_convenio_fatura_convenio_id ON item_fatura_convenio(fatura_convenio_id);
CREATE INDEX idx_item_fatura_convenio_item_conta_receber_id ON item_fatura_convenio(item_conta_receber_id);
