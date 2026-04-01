-- Adicionar relacionamento de todos com usuários
ALTER TABLE todos ADD COLUMN user_id BIGINT;

-- Adicionar constraint de foreign key
ALTER TABLE todos
    ADD CONSTRAINT fk_todos_user
    FOREIGN KEY (user_id)
    REFERENCES users(id)
    ON DELETE CASCADE;

-- Adicionar coluna updated_at que estava faltando
ALTER TABLE todos ADD COLUMN updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;

-- Tornar user_id NOT NULL depois que a coluna foi criada
-- (se houver dados existentes, precisaria de um usuário padrão)
-- Por enquanto mantemos NULL para compatibilidade

-- Índice para performance em queries por usuário
CREATE INDEX idx_todos_user_id ON todos(user_id);
CREATE INDEX idx_todos_created_at ON todos(created_at DESC);

-- Comentários
COMMENT ON COLUMN todos.user_id IS 'ID do usuário dono da tarefa';
COMMENT ON COLUMN todos.updated_at IS 'Data da última atualização';
