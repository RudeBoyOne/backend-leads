CREATE TABLE leads (
    id SERIAL PRIMARY KEY,
    origin_lead_id VARCHAR(100) NOT NULL UNIQUE,
    origin_listing_id VARCHAR(100),
    client_listing_id VARCHAR(100),
    name VARCHAR(150),
    email VARCHAR(150),
    ddd VARCHAR(5),
    phone VARCHAR(20),
    message TEXT,
    temperature VARCHAR(20),
    transaction_type VARCHAR(50),
    timestamp TIMESTAMPTZ DEFAULT now()
);
