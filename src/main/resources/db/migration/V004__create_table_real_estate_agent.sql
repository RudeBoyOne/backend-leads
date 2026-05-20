CREATE TABLE real_estate_agent (
    id         UUID        NOT NULL,
    creci      VARCHAR(20) NOT NULL UNIQUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_real_estate_agent PRIMARY KEY (id),
    CONSTRAINT fk_real_estate_agent_user
        FOREIGN KEY (id) REFERENCES users(id) ON DELETE CASCADE
);