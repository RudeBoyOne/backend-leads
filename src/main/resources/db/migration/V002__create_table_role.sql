CREATE TABLE role (
    id              UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    name            VARCHAR(50)  NOT NULL UNIQUE,
    description     VARCHAR(150),
    active          BOOLEAN      NOT NULL DEFAULT TRUE,
    hierarchy_level INTEGER      NOT NULL DEFAULT 0,
    created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP
);

INSERT INTO role (name, description, hierarchy_level) VALUES
    ('FULL_ADMIN', 'Root do sistema', 4),
    ('ADMIN', 'Responsável da filial', 3),
    ('MANAGER', 'Gerente de equipe', 2),
    ('REAL_ESTATE_AGENT', 'Corretor imobiliário', 1),
    ('TEAM_AGENT', 'Corretor de equipe', 1),
    ('USER', 'Usuário sem papel operacional', 0);
