-- A V010 criou `team` com apenas `id`, `manager_id` e timestamps, mas a entidade
-- `Team` mapeia cinco colunas de negócio que nunca chegaram ao schema: criar,
-- listar e buscar time falhavam com `column "active" does not exist`.
ALTER TABLE team
    ADD COLUMN name        VARCHAR(100) NOT NULL,
    ADD COLUMN description VARCHAR(255),
    ADD COLUMN active      BOOLEAN      NOT NULL DEFAULT TRUE,
    ADD COLUMN max_agents  INT,
    ADD COLUMN goal_amount DOUBLE PRECISION;
