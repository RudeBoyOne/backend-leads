-- Herança JOINED: a PK da tabela filha É o id do pai (mesmo motivo do V008).
-- `branch_office_id` é mapeada uma única vez, em `users` — mesma razão do V008.
CREATE TABLE manager (
    id         UUID        PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
