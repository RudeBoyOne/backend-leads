CREATE TABLE role (
    id      UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    name    VARCHAR(50) NOT NULL UNIQUE
);

INSERT INTO role (name) VALUES
    ('FULL_ADMIN'),
    ('ADMIN'),
    ('MANAGER'),
    ('REAL_ESTATE_AGENT'),
    ('TEAM_AGENT'),
    ('USER');
