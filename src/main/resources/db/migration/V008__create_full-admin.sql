INSERT INTO users (id, role_id, name, email, password, active, created_at, updated_at)
VALUES (
    gen_random_uuid(),
    (SELECT id FROM role WHERE name = 'FULL_ADMIN'),
    'Super Admin',
    'admin@semesperanca.com.br',
    '$2a$12$92nudaVPDYuqzwCFtMnhe.WRmBMGBxs5oPHdy6BUMVlacJuGQdRKS',
    true,
    NOW(),
    NOW()
)
ON CONFLICT (email) DO NOTHING;

INSERT INTO real_estate_agent (id, creci, created_at, updated_at)
SELECT id, 'FULL-ADMIN-001', NOW(), NOW()
FROM users
WHERE email = 'admin@semesperanca.com.br'
ON CONFLICT (id) DO NOTHING;
