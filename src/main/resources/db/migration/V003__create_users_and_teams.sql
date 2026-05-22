CREATE TABLE "user" (
    id                      UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    real_estate_agency_id   UUID            NOT NULL REFERENCES real_estate_agency(id) ON DELETE CASCADE,
    role_id                 UUID            NOT NULL REFERENCES role(id),
    name                    VARCHAR(150)    NOT NULL,
    email                   VARCHAR(150)    NOT NULL,
    password                VARCHAR(255)    NOT NULL,
    active                  BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at              TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    UNIQUE (real_estate_agency_id, email)
);

CREATE TABLE manager (
    id              UUID    PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID    NOT NULL UNIQUE REFERENCES "user"(id) ON DELETE CASCADE,
    branch_office   VARCHAR(100),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE team (
    id              UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    manager_id      UUID            NOT NULL REFERENCES manager(id) ON DELETE CASCADE,
    branch_office   VARCHAR(100),
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

CREATE TABLE real_estate_agent (
    id              UUID    PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID    NOT NULL UNIQUE REFERENCES "user"(id) ON DELETE CASCADE,
    creci           VARCHAR(30),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE team_agent (
    team_id              UUID        NOT NULL REFERENCES team(id) ON DELETE CASCADE,
    real_estate_agent_id UUID        NOT NULL REFERENCES real_estate_agent(id) ON DELETE CASCADE,
    joined_at            TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (team_id, real_estate_agent_id)
);
