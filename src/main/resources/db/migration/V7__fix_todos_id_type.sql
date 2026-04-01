-- Corrigir tipo da coluna id na tabela todos de SERIAL para BIGSERIAL
-- Para compatibilidade com JPA (Long -> BIGINT)

ALTER TABLE todos
    ALTER COLUMN id TYPE BIGINT;

COMMENT ON COLUMN todos.id IS 'ID único da tarefa (BIGINT para compatibilidade JPA)';
