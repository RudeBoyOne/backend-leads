-- Herança JOINED: a PK da tabela filha É o id do pai. Uma coluna `user_id`
-- separada quebraria o mapeamento — o Hibernate escreve `id` e nunca
-- `user_id`, que ficaria NOT NULL sem valor em todo insert.
CREATE TABLE real_estate_agent (
    id               UUID        PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
    branch_office_id UUID        REFERENCES branch_office(id) ON DELETE SET NULL,
    creci            VARCHAR(30),
    created_at       TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_agent_branch_office ON real_estate_agent(branch_office_id);
