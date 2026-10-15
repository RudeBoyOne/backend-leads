-- `branch_office_id` NÃO é criada aqui: `branch_office` também referencia
-- `branch_owner`, que referencia `users`. Criar inline fecharia o ciclo
-- users -> branch_office -> branch_owner -> users, que o Postgres não resolve
-- com REFERENCES em CREATE TABLE. A coluna entra como ALTER na V020.
CREATE TABLE users (
    id                      UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    real_estate_agency_id   UUID         NOT NULL REFERENCES real_estate_agency(id) ON DELETE CASCADE,
    role_id                 UUID         NOT NULL REFERENCES role(id),
    name                    VARCHAR(150) NOT NULL,
    email                   VARCHAR(150) NOT NULL,
    password                VARCHAR(255) NOT NULL,
    active                  BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at              TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMPTZ  NOT NULL DEFAULT NOW(),

    -- E-mail é UNIQUE global, e não por agência: o `AuthService.login` busca por
    -- e-mail sozinho e devolve um único `User`. A chave (agência, e-mail) deixaria
    -- dois logins apontando para o mesmo e-mail.
    UNIQUE (email)
);

CREATE INDEX idx_users_agency ON users(real_estate_agency_id);
