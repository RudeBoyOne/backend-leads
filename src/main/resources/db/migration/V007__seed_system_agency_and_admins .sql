-- Imobiliária padrão do sistema
INSERT INTO real_estate_agency (id, name, cnpj, email, phone, active)
VALUES (
    gen_random_uuid(),
    'Sistema',
    '00.000.000/0000-00',
    'sistema@backendleads.com.br',
    '00000000000',
    true
);

INSERT INTO "users" (id, real_estate_agency_id, role_id, name, email, password, active)
VALUES (
    gen_random_uuid(),
    (SELECT id FROM real_estate_agency WHERE email = 'sistema@backendleads.com.br'),
    (SELECT id FROM role WHERE name = 'FULL_ADMIN'),
    'Super Admin',
    'admin@semesperanca.com.br',
    '$2a$10$dMqgBr5qXfAyCdDhIWdnN.9jpemeIANoL7YYbwjA4TEv6bubaDlcm',
    true
);

INSERT INTO real_estate_agent (id, creci)
VALUES (
    (SELECT id FROM "users" WHERE email = 'admin@semesperanca.com.br'),
    'FULL-ADMIN-001'
);

INSERT INTO "users" (id, real_estate_agency_id, role_id, name, email, password, active)
VALUES (
    gen_random_uuid(),
    (SELECT id FROM real_estate_agency WHERE email = 'sistema@backendleads.com.br'),
    (SELECT id FROM role WHERE name = 'FULL_ADMIN'),
    'Elliot',
    'mrRobo@gmail.com',
    '$2a$10$XMwVHzL717TnAsqqoTUNlOUGuy1xiMNcH.F7y8ezEs02fnOiNZLFy',
    true
);

INSERT INTO real_estate_agent (id, creci)
VALUES (
    (SELECT id FROM "users" WHERE email = 'mrRobo@gmail.com'),
    'FULL-ADMIN-ELLIOT'
);