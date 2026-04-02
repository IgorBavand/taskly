-- V9__add_face_embedding_to_users.sql
-- Adiciona coluna para armazenar embedding facial (DeepFace)

-- Adiciona coluna para armazenar embedding facial como string CSV
-- Embedding é um vetor numérico que representa características únicas do rosto
-- Armazenado como TEXT ao invés da imagem para:
-- - Privacidade (não armazena foto)
-- - Economia de espaço
-- - Performance (comparação mais rápida)
ALTER TABLE users
ADD COLUMN face_embedding TEXT NULL;

-- Adiciona comentário explicativo
COMMENT ON COLUMN users.face_embedding IS 'Face embedding vector (CSV format) from DeepFace for facial recognition';
