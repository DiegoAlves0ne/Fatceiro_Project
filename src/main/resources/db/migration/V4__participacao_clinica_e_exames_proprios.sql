ALTER TABLE titulo ADD COLUMN percentual_clinica NUMERIC(5,4);
-- Adota a nova regra também nos atendimentos previamente registrados.
UPDATE titulo SET percentual_clinica=0.3000 WHERE categoria='CONSULTA';
UPDATE titulo SET percentual_clinica=0.0000, medico_id=NULL WHERE categoria='EXAME';
DROP INDEX uk_faturamento_atendimento;
CREATE UNIQUE INDEX uk_faturamento_atendimento ON titulo(atendimento_id) WHERE categoria IS NOT NULL;
ALTER TABLE titulo ADD CONSTRAINT ck_participacao_clinica CHECK (
 categoria IS NULL OR (
 tipo='RECEBER' AND atendimento_id IS NOT NULL AND data_competencia IS NOT NULL
 AND percentual_clinica IS NOT NULL AND (
  (categoria='CONSULTA' AND medico_id IS NOT NULL AND percentual_clinica=0.3000)
  OR (categoria='EXAME' AND medico_id IS NULL AND percentual_clinica=0.0000)
 ))
);
