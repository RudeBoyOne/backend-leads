CREATE TABLE leads (
    id SERIAL PRIMARY KEY,
    origin_lead_id VARCHAR(100) NOT NULL UNIQUE,
    origin_listing_id VARCHAR(100) NOT NULL,
    lead_origin VARCHAR(255) NOT NULL,
    client_listing_id VARCHAR(100) NOT NULL,
    name VARCHAR(150) NOT NULL,
    email VARCHAR(150) NOT NULL,
    ddd VARCHAR(5) NOT NULL,
    phone VARCHAR(20) NOT NULL,
    message TEXT NOT NULL,
    temperature VARCHAR(20) NOT NULL,
    transaction_type VARCHAR(50) NOT NULL,
    timestamp TIMESTAMPTZ NOT NULL DEFAULT now()
);