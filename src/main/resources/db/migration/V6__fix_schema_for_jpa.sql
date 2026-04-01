-- Ajustes no schema para compatibilidade com JPA

-- 1. Aumentar tamanho do campo token na tabela refresh_tokens
-- De VARCHAR(255) para VARCHAR(512) para suportar tokens maiores
ALTER TABLE refresh_tokens
    ALTER COLUMN token TYPE VARCHAR(512);

-- 2. Remover todos existentes com user_id NULL (dados legados antes da implementação de auth)
DELETE FROM todos WHERE user_id IS NULL;

-- 3. Tornar user_id NOT NULL na tabela todos
ALTER TABLE todos
    ALTER COLUMN user_id SET NOT NULL;

-- 3. Adicionar coluna updated_at à tabela refresh_tokens (para consistência com BaseEntity)
ALTER TABLE refresh_tokens
    ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;

-- Comentários
COMMENT ON COLUMN refresh_tokens.token IS 'Token criptograficamente seguro (até 512 caracteres)';
COMMENT ON COLUMN refresh_tokens.updated_at IS 'Data da última atualização do token';
COMMENT ON COLUMN todos.user_id IS 'ID do usuário dono da tarefa (obrigatório)';
