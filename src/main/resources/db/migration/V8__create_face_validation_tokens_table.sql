-- Tabela de tokens de validação facial
CREATE TABLE face_validation_tokens (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    token VARCHAR(255) UNIQUE NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    used BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_face_validation_tokens_user
        FOREIGN KEY (user_id)
        REFERENCES users(id)
        ON DELETE CASCADE
);

-- Índices para performance
CREATE INDEX idx_face_validation_tokens_token ON face_validation_tokens(token);
CREATE INDEX idx_face_validation_tokens_user_id ON face_validation_tokens(user_id);
CREATE INDEX idx_face_validation_tokens_expires_at ON face_validation_tokens(expires_at);
CREATE INDEX idx_face_validation_tokens_used ON face_validation_tokens(used);

-- Comentários
COMMENT ON TABLE face_validation_tokens IS 'Tokens temporários emitidos após validação facial bem-sucedida';
COMMENT ON COLUMN face_validation_tokens.token IS 'Token único para validação facial (UUID)';
COMMENT ON COLUMN face_validation_tokens.expires_at IS 'Data de expiração do token (2 minutos)';
COMMENT ON COLUMN face_validation_tokens.used IS 'Indica se o token já foi consumido';
