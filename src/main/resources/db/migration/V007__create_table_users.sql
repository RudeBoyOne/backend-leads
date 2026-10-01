CREATE TABLE users (
    id                      UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    real_estate_agency_id   UUID         NOT NULL REFERENCES real_estate_agency(id) ON DELETE CASCADE,
    branch_office_id        UUID         REFERENCES branch_office(id) ON DELETE SET NULL,
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

CREATE INDEX idx_users_agency        ON users(real_estate_agency_id);
CREATE INDEX idx_users_branch_office ON users(branch_office_id);
