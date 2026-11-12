ALTER TABLE titulo ADD COLUMN compra_estoque_id UUID;
ALTER TABLE titulo ADD COLUMN fornecedor_id UUID;
ALTER TABLE titulo ADD CONSTRAINT uk_titulo_compra_estoque UNIQUE (compra_estoque_id);
ALTER TABLE titulo ADD CONSTRAINT ck_titulo_compra CHECK (
 compra_estoque_id IS NULL OR (tipo = 'PAGAR' AND fornecedor_id IS NOT NULL)
);

-- Não inventar a forma dos registros anteriores à migração.
ALTER TABLE pagamento ADD COLUMN forma VARCHAR(30) NOT NULL DEFAULT 'NAO_INFORMADA';
ALTER TABLE pagamento ALTER COLUMN forma DROP DEFAULT;
ALTER TABLE pagamento ADD CONSTRAINT ck_pagamento_forma CHECK (
 forma IN ('PIX','DINHEIRO','CARTAO_CREDITO','CARTAO_DEBITO','BOLETO','TRANSFERENCIA','NAO_INFORMADA')
);
