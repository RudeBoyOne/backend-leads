-- A filial do time NÃO é uma coluna aqui. Todo time tem gerente obrigatório
-- e todo gerente está lotado em exatamente uma filial, então a filial do time
-- é `manager.branch_office_id`. Denormalizá-la aqui criaria dois endereços
-- para o mesmo fato, que divergem no primeiro update parcial — e é a ambiguidade
-- que fazia o escopo do branch owner ter dois caminhos possíveis até o time.
CREATE TABLE team (
    id         UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    manager_id UUID        NOT NULL REFERENCES manager(id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_team_manager ON team(manager_id);
