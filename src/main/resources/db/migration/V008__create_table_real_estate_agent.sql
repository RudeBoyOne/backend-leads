-- Herança JOINED: a PK da tabela filha É o id do pai. Uma coluna `user_id`
-- separada quebraria o mapeamento — o Hibernate escreve `id` e nunca
-- `user_id`, que ficaria NOT NULL sem valor em todo insert.
--
-- `branch_office_id` NÃO mora aqui: `User` a declara uma única vez, mapeada
-- em `users.branch_office_id`. Uma cópia na tabela filha seria uma coluna que
-- o Hibernate nunca escreve (não há setter) e que `findByBranchOfficeId`
-- nunca lê, porque o Criteria resolve o atributo herdado na tabela pai.
CREATE TABLE real_estate_agent (
    id         UUID        PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
    creci      VARCHAR(30),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
