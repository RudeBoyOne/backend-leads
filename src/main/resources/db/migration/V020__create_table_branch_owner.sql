-- O responsável (branch owner) é o ancestral de toda a hierarquia operacional:
--
--   FULL_ADMIN          root do software, externo ao negócio
--     └─ ADMIN          branch owner  -> suas filiais
--          └─ MANAGER   -> seus times -> seus corretores
--               └─ REAL_ESTATE_AGENT  -> seus leads
--
-- `branch_owner` é subclasse JOINED de `users`, como `manager` e
-- `real_estate_agent`. Todo campo de pessoa mora em `users`; aqui fica só
-- `phone`, que `users` não tem. A role do dono é ADMIN (V002), e é a única
-- pessoa que carrega essa role.
--
-- `users.branch_office_id` fica NULL para o branch owner: ele supervisiona N
-- filiais, então não tem uma "sede". As filiais dele vêm da coleção inversa
-- `BranchOwner.branchOffices`, nunca de uma coluna única.
CREATE TABLE branch_owner (
    id         UUID        PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
    phone      VARCHAR(20),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Fecha o ciclo users -> branch_office -> branch_owner -> users. Nenhuma
-- dessas três FKs pode ser inline em CREATE TABLE, porque as três tabelas se
-- referenciam: só a ordem de criação não resolve.
--
-- A coluna é NOT NULL, e é ela que materializa o N:1 do lado da filial — uma
-- filial tem exatamente um responsável, sempre. RESTRICT (e não CASCADE) porque
-- remover um responsável não pode deixar uma filial órfã nem derrubar os times
-- dentro dela.
--
-- O índice é comum, e não UNIQUE, e isso é o que faz o outro lado do N:1 existir.
-- Um UNIQUE em (branch_owner_id) limitaria o responsável a UMA filial — que é
-- 1:1, não 1:N, e brigaria com `BranchOwner.branchOffices` (coleção) e com a
-- subquery `RoleScope.ownedOffices`, que espera um conjunto de filiais. Quem
-- realmente exige unicidade aqui é o nome da filial dentro da agência, que já
-- tem índice próprio.
ALTER TABLE users
    ADD COLUMN branch_office_id UUID REFERENCES branch_office(id) ON DELETE SET NULL;

CREATE INDEX idx_users_branch_office ON users(branch_office_id);

ALTER TABLE branch_office
    ADD COLUMN branch_owner_id UUID NOT NULL REFERENCES branch_owner(id) ON DELETE RESTRICT;

CREATE INDEX idx_branch_office_branch_owner ON branch_office(branch_owner_id);

-- Seed mínimo da hierarquia, para existir um login com que validar o escopo
-- depois de subir a aplicação. Sem um branch owner semeado não há como
-- exercitar o corte de escopo sem criar tudo pela API primeiro.
-- Ordem importa: users -> branch_owner -> branch_office, porque
-- branch_office.branch_owner_id é NOT NULL e branch_owner.id referencia users.
INSERT INTO users (id, real_estate_agency_id, role_id, name, email, password, active)
VALUES (
    gen_random_uuid(),
    (SELECT id FROM real_estate_agency WHERE email = 'sistema@backendleads.com.br'),
    (SELECT id FROM role WHERE name = 'ADMIN'),
    'Marina Reis',
    'marina@semesperanca.com.br',
    '$2a$10$dMqgBr5qXfAyCdDhIWdnN.9jpemeIANoL7YYbwjA4TEv6bubaDlcm',
    true
);

INSERT INTO branch_owner (id, phone)
VALUES (
    (SELECT id FROM users WHERE email = 'marina@semesperanca.com.br'),
    '11988887777'
);

INSERT INTO branch_office (id, real_estate_agency_id, branch_owner_id, name, address, phone, active)
VALUES (
    gen_random_uuid(),
    (SELECT id FROM real_estate_agency WHERE email = 'sistema@backendleads.com.br'),
    (SELECT id FROM branch_owner WHERE id = (SELECT id FROM users WHERE email = 'marina@semesperanca.com.br')),
    'Filial Centro',
    'Rua das Flores, 100',
    '1133334444',
    true
);

