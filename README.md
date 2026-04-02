# Taskly API 🚀

API REST moderna e segura para gerenciamento de tarefas, construída com Kotlin, Javalin e PostgreSQL, seguindo princípios de DDD (Domain-Driven Design) e melhores práticas de segurança.

## 🎯 Características

### Funcionalidades
- ✅ Autenticação JWT com Refresh Tokens
- ✅ RBAC (Role-Based Access Control)
- ✅ CRUD completo de tarefas
- ✅ **Validação Facial com DeepFace** (opcional para criação de tarefas)
- ✅ Rate Limiting
- ✅ Auditoria de segurança
- ✅ Isolamento de dados por usuário

### Segurança
- 🔐 JWT (HMAC256) com expiração configurável
- 🔒 BCrypt para hashing de senhas (12 rounds)
- 🛡️ Proteção contra SQL Injection (Prepared Statements)
- 🚦 Rate Limiting (global e por endpoint)
- 📝 Logs de auditoria detalhados
- 🔑 Refresh Token Rotation
- 🌐 Headers HTTP de segurança (HSTS, CSP, X-Frame-Options, etc)
- ⚡ Validação e sanitização de inputs

### Arquitetura
- 📦 DDD Simplificado (Domain, Application, Infrastructure, Presentation)
- 🎯 Separação de responsabilidades
- 🔄 Dependency Injection manual
- 🗃️ Repository Pattern
- 🎨 Value Objects (Email, etc)
- 📊 Migrations com Flyway

## 🏗️ Estrutura do Projeto

```
src/main/kotlin/
├── config/                    # Configurações (Javalin, Database, Env)
├── domain/
│   ├── entity/               # Entidades de domínio (User, Todo, RefreshToken, FaceValidationToken)
│   └── repository/           # Interfaces de repositórios
├── application/
│   └── service/              # Lógica de negócio (AuthService, TodoService, FaceValidationService)
├── infrastructure/
│   ├── repository/           # Implementações JPA dos repositórios
│   └── external/             # Integração com serviços externos (DeepFaceService)
├── presentation/
│   ├── controller/           # Controllers REST
│   └── dto/                  # Data Transfer Objects
├── security/                 # JWT, BCrypt, Middleware, Rate Limiter
└── Main.kt                   # Ponto de entrada

src/main/resources/
└── db/migration/             # Migrations Flyway

deepface-service/             # Microserviço Python de reconhecimento facial
├── app/
│   └── main.py              # FastAPI app
├── requirements.txt
├── Dockerfile
└── README.md
```

## 🚀 Início Rápido

### Pré-requisitos
- Java 21+
- Maven 3.6+
- PostgreSQL 12+
- Docker & Docker Compose (recomendado)
- Python 3.11+ (se executar DeepFace localmente)

### 1. Clonar repositório
```bash
git clone https://github.com/seu-usuario/taskly.git
cd taskly
```

### 2. Configurar banco de dados
```bash
# Criar banco de dados
createdb taskly

# Ou via psql
psql -U postgres -c "CREATE DATABASE taskly;"
```

### 3. Configurar variáveis de ambiente
```bash
# Copiar arquivo de exemplo
cp .env.example .env

# Editar .env com suas configurações
nano .env
```

**Variáveis obrigatórias:**
```bash
DATABASE_PASSWORD=sua_senha_postgres
JWT_SECRET=chave_256_bits_minimo_aleatoria
```

**Gerar JWT_SECRET seguro:**
```bash
openssl rand -base64 32
```

### 4. Instalar dependências
```bash
mvn clean install
```

### 5. Executar aplicação
```bash
mvn exec:java
```

A API estará disponível em: `http://localhost:7171`

### 6. (Opcional) Executar com Docker Compose

Para executar a stack completa (PostgreSQL + Backend + DeepFace Service):

```bash
# Configurar variáveis de ambiente
cp .env.example .env
nano .env  # Ajustar configurações

# Iniciar todos os serviços
docker-compose up -d

# Verificar logs
docker-compose logs -f

# Parar serviços
docker-compose down
```

Serviços disponíveis:
- Backend API: `http://localhost:7171`
- DeepFace Service: `http://localhost:8080`
- PostgreSQL: `localhost:5432`

## 📖 Documentação

- **[API.md](./API.md)** - Documentação completa dos endpoints (se disponível)
- **[DEEPFACE_MIGRATION.md](./DEEPFACE_MIGRATION.md)** - Guia de migração CompreFace → DeepFace
- **[deepface-service/README.md](./deepface-service/README.md)** - Documentação do serviço DeepFace

## 🎭 Validação Facial

Este projeto inclui validação facial opcional usando DeepFace.

### Como funciona

1. **Registro de Face** (primeira vez):
   ```bash
   POST /api/v1/face/register
   ```
   - Frontend captura foto do usuário
   - Backend extrai embedding (vetor de 512 dimensões)
   - Embedding é armazenado no banco (não a imagem)

2. **Validação Facial** (antes de criar tarefa):
   ```bash
   POST /api/v1/face/verify
   ```
   - Frontend captura foto novamente
   - Backend compara com embedding armazenado
   - Se verificado, emite token válido por 2 minutos

3. **Criar Tarefa** (com validação):
   ```bash
   POST /api/v1/todos
   Headers: { "X-Face-Validation-Token": "abc123" }
   ```
   - Middleware valida token facial
   - Se válido, permite criação da tarefa

### Vantagens

- **Privacidade**: Armazena apenas embeddings (vetores), não fotos
- **Performance**: Verificação em ~50ms
- **Compatibilidade**: Roda nativamente em Apple Silicon (ARM64)
- **Segurança**: Tokens expiram em 2 minutos
- **LGPD/GDPR**: Embeddings não permitem reconstrução da face

### Configuração

Veja [DEEPFACE_MIGRATION.md](./DEEPFACE_MIGRATION.md) para:
- Setup do DeepFace service
- Ajuste de thresholds
- Troubleshooting
- Comparação de modelos

### Exemplo rápido

```bash
# 1. Registrar usuário
curl -X POST http://localhost:7070/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{"email":"user@example.com","password":"SecurePass123"}'

# 2. Login
curl -X POST http://localhost:7070/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"user@example.com","password":"SecurePass123"}'

# 3. Criar tarefa (use o token do login)
curl -X POST http://localhost:7070/api/v1/todos \
  -H "Authorization: Bearer SEU_TOKEN_AQUI" \
  -H "Content-Type: application/json" \
  -d '{"title":"Minha primeira tarefa"}'
```

## 🔒 Segurança

Este projeto implementa as recomendações do OWASP Top 10. Veja [SECURITY.md](./SECURITY.md) para detalhes.

### Principais medidas
- Autenticação JWT stateless
- Refresh token com rotação automática
- Passwords com BCrypt (12 rounds)
- Rate limiting (5 req/min para login)
- SQL Injection protection (Prepared Statements)
- XSS protection headers
- CORS configurável
- Auditoria completa

## 🗄️ Banco de Dados

### Migrations

As migrations são executadas automaticamente ao iniciar a aplicação.

```sql
-- Tabelas criadas:
users           -- Usuários do sistema
todos           -- Tarefas dos usuários
refresh_tokens  -- Tokens de renovação
```

### Executar migrations manualmente
```bash
mvn flyway:migrate
```

### Rollback (desenvolvimento)
```bash
mvn flyway:clean  # ⚠️ APAGA TODOS OS DADOS
mvn flyway:migrate
```

## 🧪 Testes

```bash
# Executar testes
mvn test

# Executar com coverage
mvn test jacoco:report
```

## 📊 Monitoramento

### Health Check
```bash
curl http://localhost:7070/health
```

### Logs de Auditoria

Os logs de segurança são registrados com o prefixo `[AUDIT]`:

```
2024-01-01 12:00:00 [AUDIT] AUTH_SUCCESS | email=user@example.com | ip=192.168.1.1
2024-01-01 12:01:00 [AUDIT] AUTH_FAILURE | email=hacker@evil.com | ip=10.0.0.1 | reason=invalid_password
```

## 🌍 Variáveis de Ambiente

| Variável | Obrigatória | Padrão | Descrição |
|----------|-------------|--------|-----------|
| `ENVIRONMENT` | Não | development | Ambiente (development/production) |
| `SERVER_PORT` | Não | 7070 | Porta do servidor |
| `DATABASE_URL` | Não | jdbc:postgresql://localhost:5432/taskly | URL do banco |
| `DATABASE_USER` | Não | postgres | Usuário do banco |
| `DATABASE_PASSWORD` | Sim (prod) | postgres | Senha do banco |
| `JWT_SECRET` | Sim (prod) | auto | Chave secreta JWT (256+ bits) |
| `JWT_EXPIRATION_MS` | Não | 3600000 | Expiração do JWT (ms) |
| `REFRESH_TOKEN_EXPIRATION_DAYS` | Não | 30 | Expiração do refresh token (dias) |
| `BCRYPT_ROUNDS` | Não | 12 | Custo do BCrypt |
| `ALLOWED_ORIGINS` | Não | http://localhost:3000 | Origens CORS permitidas |
| `DEEPFACE_URL` | Não | http://localhost:8080 | URL do serviço DeepFace |
| `DEEPFACE_THRESHOLD` | Não | 0.4 | Threshold de verificação facial |

## 🚢 Deploy

### Docker (recomendado)

```bash
# Build
docker build -t taskly-api .

# Run
docker run -p 7070:7070 \
  -e DATABASE_URL=jdbc:postgresql://host.docker.internal:5432/taskly \
  -e DATABASE_PASSWORD=secret \
  -e JWT_SECRET=your-secret-key \
  -e ENVIRONMENT=production \
  taskly-api
```

### JAR Executável

```bash
# Compilar
mvn clean package

# Executar
java -jar target/Taskly-1.0-SNAPSHOT.jar
```

## 🛠️ Tecnologias

### Backend (Kotlin)
- **Kotlin** 1.9.22 - Linguagem
- **Javalin** 5.6.1 - Framework web
- **PostgreSQL** 42.7.3 - Banco de dados
- **Hibernate/JPA** 6.4.4 - ORM
- **Flyway** 9.22.3 - Migrations
- **HikariCP** 5.1.0 - Connection pooling
- **JWT** (auth0) 4.4.0 - Autenticação
- **BCrypt** 0.4 - Hash de senhas
- **Jackson** 2.15.2 - JSON serialization
- **OkHttp** 4.12.0 - HTTP client
- **SLF4J** 2.0.9 - Logging

### Face Recognition (Python)
- **DeepFace** 0.0.93 - Framework de reconhecimento facial
- **FastAPI** 0.115.0 - API web framework
- **TensorFlow** 2.18.0 - Machine Learning (compatível ARM64)
- **OpenCV** 4.10.0 - Processamento de imagem
- **Facenet512** - Modelo de reconhecimento (padrão)

## 🤝 Contribuindo

1. Fork o projeto
2. Crie uma branch (`git checkout -b feature/nova-feature`)
3. Commit suas mudanças (`git commit -am 'Adiciona nova feature'`)
4. Push para a branch (`git push origin feature/nova-feature`)
5. Abra um Pull Request

## 📝 License

Este projeto está sob a licença MIT. Veja [LICENSE](./LICENSE) para mais detalhes.

## 👤 Autor

Igor Guerreiro

## 🙏 Agradecimentos

- OWASP pela documentação de segurança
- Javalin pela simplicidade
- Kotlin pelo poder e elegância

---

**⚠️ IMPORTANTE:** Nunca commite o arquivo `.env` com credenciais reais!
