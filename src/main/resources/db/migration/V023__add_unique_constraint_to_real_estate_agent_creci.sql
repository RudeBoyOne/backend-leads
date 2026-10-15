-- `RealEstateAgent.creci` declara `unique = true`, mas o `V002` criou a coluna
-- como `VARCHAR(30)` solta — e `ddl-auto: validate` só confere existência e
-- tipo de coluna, nunca constraint. A única barreira era o
-- `RealEstateAgentRepository.existsByCreci`, que é checagem de aplicação e
-- corre em corrida entre o check e o insert.
--
-- O `NOT NULL` que a entidade também declara fica de fora de propósito:
-- `creci` pode ter NULL em bases já populadas e travar a migration aqui seria
-- pior do que a divergência. UNIQUE aceita vários NULL no PostgreSQL, então
-- esta constraint é segura mesmo com a coluna anulável.
--
-- Se a base já tiver CRECI repetido, a migration falha — o que é o comportamento
-- desejado: a duplicata precisa ser resolvida antes de virar constraint.

ALTER TABLE real_estate_agent
    ADD CONSTRAINT uq_real_estate_agent_creci UNIQUE (creci);
