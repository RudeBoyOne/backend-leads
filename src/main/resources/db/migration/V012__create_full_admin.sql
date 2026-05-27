INSERT INTO users (id, role_id, name, email, password, active, created_at, updated_at)
VALUES (
    gen_random_uuid(),
    (SELECT id FROM role WHERE name = 'FULL_ADMIN'),
    'Elliot',
    'mrRobo@gmail.com',
    '$2a$10$kFWuu11Pzhtdh7P8/M4ASu33wJ0TSezo5Sl2in1c91ewavDF2MwdG',
    true,
    NOW(),
    NOW()
);

INSERT INTO real_estate_agent (id, creci, created_at, updated_at)
VALUES (
    (SELECT id FROM users WHERE email = 'mrRobo@gmail.com'),
    'FULL-ADMIN-ELLIOT',
    NOW(),
    NOW()
);