-- Tabela de refresh tokens
CREATE TABLE refresh_tokens (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    token VARCHAR(255) UNIQUE NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    revoked BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_refresh_tokens_user
        FOREIGN KEY (user_id)
        REFERENCES users(id)
        ON DELETE CASCADE
);

-- Índices para performance
CREATE INDEX idx_refresh_tokens_token ON refresh_tokens(token);
CREATE INDEX idx_refresh_tokens_user_id ON refresh_tokens(user_id);
CREATE INDEX idx_refresh_tokens_expires_at ON refresh_tokens(expires_at);

-- Comentários
COMMENT ON TABLE refresh_tokens IS 'Tokens de refresh para renovação de JWT';
COMMENT ON COLUMN refresh_tokens.token IS 'Token criptograficamente seguro (256 bits)';
COMMENT ON COLUMN refresh_tokens.expires_at IS 'Data de expiração do token';
COMMENT ON COLUMN refresh_tokens.revoked IS 'Indica se o token foi revogado (logout)';
