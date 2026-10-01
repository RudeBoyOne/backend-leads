-- Herança JOINED: a PK da tabela filha É o id do pai (mesmo motivo do V008).
CREATE TABLE manager (
    id               UUID        PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
    branch_office_id UUID        REFERENCES branch_office(id) ON DELETE SET NULL,
    created_at       TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_manager_branch_office ON manager(branch_office_id);
