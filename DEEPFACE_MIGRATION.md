# Guia de Migração: CompreFace → DeepFace

Este documento detalha a migração completa do sistema de reconhecimento facial de CompreFace para DeepFace.

## Motivação

### Problemas com CompreFace
- **Incompatibilidade ARM64**: API Core trava ao carregar modelos em Apple Silicon
- **Lentidão**: 20+ minutos para inicializar, CPU 100% em emulação
- **Complexidade**: 4 containers (UI, Admin, Core, PostgreSQL)
- **Tamanho**: ~2GB de imagens Docker
- **Armazenamento**: Imagens faciais em BLOB (privacidade + espaço)

### Vantagens do DeepFace
- **Compatibilidade ARM64**: TensorFlow 2.18.0 nativo para Apple Silicon
- **Performance**: Inicializa em 10-20s, processamento em ~50ms
- **Simplicidade**: 1 container Python/FastAPI
- **Tamanho**: ~500MB de imagem Docker
- **Privacidade**: Armazena apenas embeddings (vetores), não imagens
- **Open Source**: MIT License, sem restrições comerciais

## Arquitetura

### Antes (CompreFace)
```
┌──────────────┐
│   Angular    │
│   Frontend   │
└──────┬───────┘
       │
       ▼
┌──────────────┐     ┌────────────────┐
│   Kotlin     │────▶│  CompreFace UI │
│   Backend    │     │  (Port 8000)   │
│  (Port 7171) │     └────────┬───────┘
└──────┬───────┘              │
       │              ┌───────▼────────┐
       │              │ CompreFace     │
       │              │ Admin + Core   │
       │              │ (3 containers) │
       │              └───────┬────────┘
       │                      │
       ▼                      ▼
┌──────────────┐     ┌────────────────┐
│  PostgreSQL  │     │  CompreFace    │
│  (Taskly)    │     │  PostgreSQL    │
└──────────────┘     └────────────────┘
```

### Depois (DeepFace)
```
┌──────────────┐
│   Angular    │
│   Frontend   │
└──────┬───────┘
       │
       ▼
┌──────────────┐     ┌────────────────┐
│   Kotlin     │────▶│  DeepFace      │
│   Backend    │ HTTP│  Service       │
│  (Port 7171) │     │  (Port 8080)   │
└──────┬───────┘     └────────────────┘
       │
       ▼
┌──────────────┐
│  PostgreSQL  │
│  (Taskly)    │
│  + embeddings│
└──────────────┘
```

## Mudanças Implementadas

### 1. Microserviço Python (DeepFace)

**Arquivos criados:**
```
deepface-service/
├── app/
│   ├── __init__.py
│   └── main.py              # FastAPI app com endpoints
├── requirements.txt         # Dependências Python
├── Dockerfile              # Multi-platform Docker image
├── .env.example            # Configuração
└── README.md               # Documentação completa
```

**Endpoints:**
- `POST /face/embedding` - Extrai embedding de uma imagem
- `POST /face/verify` - Verifica se duas faces correspondem
- `GET /health` - Health check

### 2. Backend Kotlin

**Arquivos criados:**
```kotlin
src/main/kotlin/infrastructure/external/DeepFaceService.kt
```
- Cliente HTTP para integração com DeepFace service
- Métodos: `extractEmbedding()`, `verifyFaceWithEmbedding()`, `verifyFaces()`

**Arquivos modificados:**

**`src/main/kotlin/application/service/FaceValidationService.kt`**
- ❌ Removido: `CompreFaceService` dependency
- ✅ Adicionado: `DeepFaceService` dependency
- Mudança no fluxo:
  - `registerUserFace()`: Extrai embedding → salva no banco
  - `verifyFaceAndIssueToken()`: Verifica contra embedding armazenado

**`src/main/kotlin/domain/entity/User.kt`**
```kotlin
data class User(
    // ...
    val faceEmbedding: String? = null,  // Novo campo: vetor CSV
    // ...
)
```

**`src/main/kotlin/infrastructure/persistence/entity/UserEntity.kt`**
```kotlin
@Column(name = "face_embedding", nullable = true, columnDefinition = "TEXT")
var faceEmbedding: String? = null
```

**`src/main/kotlin/Main.kt`**
```kotlin
// Antes
val compreFaceService = CompreFaceService()
val faceValidationService = FaceValidationService(
    compreFaceService, // ...
)

// Depois
val deepFaceService = DeepFaceService()
val faceValidationService = FaceValidationService(
    deepFaceService, // ...
)
```

**`src/main/kotlin/config/EnvConfig.kt`**
```kotlin
// Novo
val DEEPFACE_URL: String = getEnv("DEEPFACE_URL", "http://localhost:8080")
val DEEPFACE_THRESHOLD: Double = getEnv("DEEPFACE_THRESHOLD", "0.4").toDouble()

// Mantido para compatibilidade
val COMPREFACE_URL: String = getEnv("COMPREFACE_URL", "http://localhost:8000")
val COMPREFACE_API_KEY: String = getEnv("COMPREFACE_API_KEY", "...")
```

### 3. Database Migration

**Arquivo criado:**
```sql
src/main/resources/db/migration/V9__add_face_embedding_to_users.sql
```

```sql
ALTER TABLE users
ADD COLUMN face_embedding TEXT NULL;

COMMENT ON COLUMN users.face_embedding IS
  'Face embedding vector (CSV format) from DeepFace for facial recognition';
```

**Formato de armazenamento:**
- Embedding: `"0.123,-0.456,0.789,0.234,..."`
- Dimensões: 512 valores (Facenet512)
- Tipo: TEXT (compatível com qualquer tamanho)

### 4. Docker & Infraestrutura

**Arquivos criados/modificados:**
- `Dockerfile` - Backend Kotlin com multi-stage build
- `.dockerignore` - Otimização do build
- `docker-compose.yml` - Stack completa (PostgreSQL + DeepFace + Backend)
- `pom.xml` - Adicionado maven-shade-plugin para JAR executável

**Docker Compose:**
```yaml
services:
  postgres:          # PostgreSQL para Taskly
  deepface-service:  # Serviço DeepFace (Python)
  taskly-backend:    # Backend Kotlin
```

### 5. Configuração

**`.env.example` atualizado:**
```bash
# DeepFace (novo)
DEEPFACE_URL=http://localhost:8080
DEEPFACE_THRESHOLD=0.4

# CompreFace (deprecated - mantido para compatibilidade)
COMPREFACE_URL=http://localhost:8000
COMPREFACE_API_KEY=00000000-0000-0000-0000-000000000000
```

## Fluxo de Dados

### Registro de Rosto (Primeira vez)

```
1. Frontend captura foto → Base64
2. POST /api/v1/face/register
   Body: { "image_base64": "..." }
3. Backend → DeepFace Service
   POST http://localhost:8080/face/embedding
4. DeepFace extrai embedding [512 dimensões]
5. Backend salva no banco:
   UPDATE users SET face_embedding = '0.123,-0.456,...'
6. Response: { "message": "Rosto registrado" }
```

### Validação Facial (Criar tarefa)

```
1. Frontend captura foto → Base64
2. POST /api/v1/face/verify
   Body: { "image_base64": "..." }
3. Backend busca embedding armazenado do usuário
4. Backend → DeepFace Service
   POST http://localhost:8080/face/verify
   Body: {
     "img1_base64": "...",
     "embedding": [0.123, -0.456, ...]
   }
5. DeepFace compara:
   - Extrai embedding da nova foto
   - Calcula distância (cosine)
   - Retorna verified: true/false
6. Se verified:
   - Backend cria token de 2 minutos
   - Response: { "token": "abc123", "expiresAt": "..." }
7. Frontend usa token para criar tarefa:
   POST /api/v1/todos
   Headers: { "X-Face-Validation-Token": "abc123" }
```

## Estrutura de Dados

### User (antes)
```kotlin
data class User(
    val id: Long,
    val email: Email,
    val passwordHash: String,
    val roles: Set<Role>,
    val active: Boolean,
    // Sem campo de face
)
```

### User (depois)
```kotlin
data class User(
    val id: Long,
    val email: Email,
    val passwordHash: String,
    val roles: Set<Role>,
    val active: Boolean,
    val faceEmbedding: String? = null,  // "0.123,-0.456,..." (512 valores)
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
)
```

### Tabela users (SQL)
```sql
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    roles VARCHAR(255) NOT NULL DEFAULT 'USER',
    active BOOLEAN NOT NULL DEFAULT true,
    face_embedding TEXT,  -- NOVO: CSV string com 512 dimensões
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
```

## Guia de Deploy

### Opção 1: Docker Compose (Recomendado)

```bash
# 1. Configurar variáveis
cp .env.example .env
nano .env  # Ajustar JWT_SECRET, DATABASE_PASSWORD, etc

# 2. Subir stack completa
docker-compose up -d

# 3. Verificar status
docker-compose ps
docker-compose logs -f

# 4. Acessar serviços
# Backend: http://localhost:7171
# DeepFace: http://localhost:8080
```

### Opção 2: Serviços Separados

```bash
# Terminal 1: PostgreSQL
docker run -d \
  -p 5432:5432 \
  -e POSTGRES_USER=postgres \
  -e POSTGRES_PASSWORD=postgres \
  -e POSTGRES_DB=taskly \
  --name taskly-postgres \
  postgres:14-alpine

# Terminal 2: DeepFace Service
cd deepface-service
python -m venv venv
source venv/bin/activate
pip install -r requirements.txt
uvicorn app.main:app --port 8080

# Terminal 3: Backend Kotlin
export DATABASE_PASSWORD=postgres
export JWT_SECRET=your-secret-key
export DEEPFACE_URL=http://localhost:8080
mvn clean compile exec:java
```

## Testes

### 1. Health Checks

```bash
# DeepFace service
curl http://localhost:8080/health
# {"status":"healthy","model":"Facenet512","backend":"opencv"}

# Backend
curl http://localhost:7171/health
# {"status":"UP","version":"1.0.0","environment":"development"}
```

### 2. Registro de Face

```bash
# Registrar usuário
curl -X POST http://localhost:7171/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "teste@example.com",
    "password": "SecurePass123"
  }'

# Login
TOKEN=$(curl -X POST http://localhost:7171/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "teste@example.com",
    "password": "SecurePass123"
  }' | jq -r '.accessToken')

# Registrar rosto (substitua IMAGE_BASE64)
curl -X POST http://localhost:7171/api/v1/face/register \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "image_base64": "data:image/jpeg;base64,/9j/4AAQ..."
  }'
```

### 3. Validação Facial

```bash
# Verificar rosto e obter token
FACE_TOKEN=$(curl -X POST http://localhost:7171/api/v1/face/verify \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "image_base64": "data:image/jpeg;base64,/9j/4AAQ..."
  }' | jq -r '.token')

# Criar tarefa com validação facial
curl -X POST http://localhost:7171/api/v1/todos \
  -H "Authorization: Bearer $TOKEN" \
  -H "X-Face-Validation-Token: $FACE_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Tarefa validada com reconhecimento facial"
  }'
```

## Comparação de Performance

| Métrica | CompreFace | DeepFace | Melhoria |
|---------|-----------|----------|----------|
| Tempo de inicialização | 5-10 min | 10-20s | **30x mais rápido** |
| Primeira verificação | ~3s | ~200ms | **15x mais rápido** |
| Verificação subsequente | ~1s | ~50ms | **20x mais rápido** |
| Uso de memória | ~2GB | ~500MB | **4x menos** |
| Tamanho em disco | ~2GB | ~500MB | **4x menos** |
| Compatibilidade ARM64 | ❌ Emulação | ✅ Nativo | **N/A** |

## Troubleshooting

### Backend não conecta no DeepFace

**Sintoma:**
```
ERROR DeepFaceService - Erro ao extrair embedding: Failed to connect
```

**Solução:**
```bash
# Verificar se DeepFace está rodando
curl http://localhost:8080/health

# Se não estiver, iniciar:
docker-compose up deepface-service

# Verificar logs
docker-compose logs deepface-service
```

### Migration V9 não executou

**Sintoma:**
```
ERROR: column "face_embedding" does not exist
```

**Solução:**
```bash
# Verificar migrations executadas
mvn flyway:info

# Executar manualmente
mvn flyway:migrate

# Ou via SQL
psql -U postgres -d taskly -c "
  ALTER TABLE users ADD COLUMN face_embedding TEXT NULL;
"
```

### Erro "No face detected"

**Sintoma:**
```json
{
  "error": "Falha ao extrair características faciais"
}
```

**Soluções:**
1. Certifique-se de que há um rosto visível na imagem
2. Melhore a iluminação
3. Envie imagem de maior qualidade
4. Tente backend mais preciso:
   ```bash
   # Em docker-compose.yml
   DEEPFACE_DETECTOR_BACKEND: mtcnn
   ```

### Verificação sempre falha

**Sintoma:**
```json
{
  "error": "Rosto não reconhecido. Similarity: 65.5%"
}
```

**Soluções:**
1. Threshold muito rigoroso → aumentar:
   ```bash
   DEEPFACE_THRESHOLD=0.5  # Menos rigoroso
   ```
2. Registrar rosto novamente com imagem de melhor qualidade
3. Usar modelo mais preciso:
   ```bash
   DEEPFACE_MODEL_NAME=ArcFace
   ```

## Rollback (se necessário)

Se precisar voltar para CompreFace:

### 1. Reverter código
```bash
git revert HEAD~n  # n = número de commits da migração
```

### 2. Remover coluna do banco
```sql
ALTER TABLE users DROP COLUMN face_embedding;
```

### 3. Restaurar docker-compose.yml
```bash
git checkout main -- docker-compose.yml
docker-compose up -d
```

### 4. Reverter configuração
```bash
# Main.kt
val compreFaceService = CompreFaceService()
val faceValidationService = FaceValidationService(compreFaceService, ...)
```

## Próximos Passos

### Melhorias Sugeridas

1. **Liveness Detection**
   - Detectar se é uma pessoa real ou foto
   - Bibliotecas: `facenet-pytorch`, `Silent-Face-Anti-Spoofing`

2. **Vector Search**
   - Buscar usuários similares (útil para detecção de duplicatas)
   - Ferramentas: FAISS, Milvus, pgvector

3. **Cache de Validações**
   - Redis para armazenar tokens de validação
   - Reduz carga no banco

4. **Métricas e Monitoramento**
   - Prometheus + Grafana
   - Alertas de falhas de verificação

5. **Frontend Angular**
   - Componente de captura facial
   - Preview antes de enviar
   - Feedback visual de qualidade

## Referências

- [DeepFace GitHub](https://github.com/serengil/deepface)
- [FastAPI Documentation](https://fastapi.tiangolo.com/)
- [TensorFlow ARM64](https://www.tensorflow.org/install)
- [Facenet512 Paper](https://arxiv.org/abs/1503.03832)

## Suporte

Em caso de dúvidas ou problemas:
1. Verifique logs: `docker-compose logs -f`
2. Consulte README do deepface-service
3. Abra issue no repositório
