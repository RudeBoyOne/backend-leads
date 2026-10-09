-- Root do software: pessoa externa ao negócio, usada para checagem, investigação
-- e dashboard de gerenciamento. Não é corretor e não é lotado em filial.
--
-- A linha fica SÓ em `users`: a raiz da herança JOINED é concreta justamente
-- para que "não é nenhum dos três subtipos" seja um estado legível. A versão
-- anterior semeava uma linha em `real_estate_agent` com CRECI `ADMIN-001` só
-- para o ORM ter o que instanciar — o admin virava um corretor sem CRECI de
-- verdade, e o escopo de `RealEstateAgent` o alcançava por acidente.
--
-- `users.branch_office_id` fica NULL: FULL_ADMIN é root, não é escopado.
INSERT INTO real_estate_agency (id, name, cnpj, email, phone, active)
VALUES (
    gen_random_uuid(),
    'Sistema',
    '00.000.000/0000-00',
    'sistema@backendleads.com.br',
    '00000000000',
    true
);

INSERT INTO users (id, real_estate_agency_id, role_id, name, email, password, active)
VALUES (
    gen_random_uuid(),
    (SELECT id FROM real_estate_agency WHERE email = 'sistema@backendleads.com.br'),
    (SELECT id FROM role WHERE name = 'FULL_ADMIN'),
    'Super Admin',
    'admin@semesperanca.com.br',
    '$2a$10$dMqgBr5qXfAyCdDhIWdnN.9jpemeIANoL7YYbwjA4TEv6bubaDlcm',
    true
);

INSERT INTO users (id, real_estate_agency_id, role_id, name, email, password, active)
VALUES (
    gen_random_uuid(),
    (SELECT id FROM real_estate_agency WHERE email = 'sistema@backendleads.com.br'),
    (SELECT id FROM role WHERE name = 'FULL_ADMIN'),
    'Elliot',
    'mrRobo@gmail.com',
    '$2a$10$XMwVHzL717TnAsqqoTUNlOUGuy1xiMNcH.F7y8ezEs02fnOiNZLFy',
    true
);
