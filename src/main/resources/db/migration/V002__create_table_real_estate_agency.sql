CREATE TABLE "users" (
    id                    UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    real_estate_agency_id UUID         REFERENCES real_estate_agency(id) ON DELETE CASCADE,
    role_id               UUID         NOT NULL REFERENCES role(id),
    name                  VARCHAR(150) NOT NULL,
    email                 VARCHAR(150) NOT NULL UNIQUE,
    password              VARCHAR(255) NOT NULL,
    active                BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at            TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at            TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE TABLE real_estate_agent (
    id         UUID        NOT NULL,
    creci      VARCHAR(30),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT pk_real_estate_agent PRIMARY KEY (id),
    CONSTRAINT fk_real_estate_agent_user
        FOREIGN KEY (id) REFERENCES "users"(id) ON DELETE CASCADE
);

CREATE TABLE manager (
    id            UUID        NOT NULL,
    branch_office VARCHAR(100),
    created_at    TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT pk_manager PRIMARY KEY (id),
    CONSTRAINT fk_manager_user
        FOREIGN KEY (id) REFERENCES "users"(id) ON DELETE CASCADE
);

CREATE TABLE team (
    id            UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    manager_id    UUID        NOT NULL REFERENCES manager(id) ON DELETE CASCADE,
    branch_office VARCHAR(100),
    created_at    TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE team_agent (
    team_id              UUID        NOT NULL REFERENCES team(id) ON DELETE CASCADE,
    real_estate_agent_id UUID        NOT NULL REFERENCES real_estate_agent(id) ON DELETE CASCADE,
    joined_at            TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (team_id, real_estate_agent_id)
);