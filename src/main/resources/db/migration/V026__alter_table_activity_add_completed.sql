-- A entidade `Activity` mapeia `completed`, mas nenhuma migration adicionava a
-- coluna: inserir, listar e buscar atividade falhavam com
-- `column "completed" does not exist`.
ALTER TABLE activity
    ADD COLUMN completed BOOLEAN NOT NULL DEFAULT FALSE;
