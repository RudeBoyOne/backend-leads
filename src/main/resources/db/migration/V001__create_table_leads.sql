CREATE TABLE IF NOT EXISTS leads
(
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    origin_lead_id VARCHAR(100) NOT NULL,
    origin_listing_id VARCHAR(100) NOT NULL,
    lead_origin VARCHAR(255) NOT NULL,
    client_listing_id VARCHAR(100) NOT NULL,
    name VARCHAR(150) NOT NULL,
    email VARCHAR(150) NOT NULL,
    phone VARCHAR(20) NOT NULL,
    message TEXT NOT NULL,
    temperature VARCHAR(20) NOT NULL,
    transaction_type VARCHAR(50) NOT NULL,
    timestamp TIMESTAMPTZ NOT NULL DEFAULT now()
)

CREATE TABLE IF NOT EXISTS users (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(255)        NOT NULL,
    email       VARCHAR(255)        NOT NULL UNIQUE,
    password    VARCHAR(255)        NOT NULL,
    role_id     VARCHAR(50)         NOT NULL DEFAULT 'USER',
    active      BOOLEAN             NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMP           NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP           NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS real_estate_agencies (
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT              NOT NULL UNIQUE,
    creci       VARCHAR(100)        NOT NULL UNIQUE,
    created_at  TIMESTAMP           NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP           NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_real_estate_agency_user
        FOREIGN KEY (user_id) REFERENCES users(id)
        ON DELETE CASCADE
);