# Quick Start - Taskly API

## 🚀 Início Rápido em 5 Minutos

### 1️⃣ Pré-requisitos

```bash
# Verificar Java
java -version  # Precisa Java 11+

# Verificar Maven
mvn -version   # Precisa Maven 3.6+

# Verificar PostgreSQL
psql --version # Precisa PostgreSQL 12+
```

### 2️⃣ Setup do Banco

```bash
# Criar banco de dados
createdb taskly

# Ou via psql
psql -U postgres -c "CREATE DATABASE taskly;"
```

### 3️⃣ Configurar Variáveis

```bash
# Copiar template
cp .env.example .env

# Gerar JWT secret seguro
echo "JWT_SECRET=$(openssl rand -base64 32)" >> .env

# Editar .env (configurar DATABASE_PASSWORD)
nano .env
```

**Conteúdo mínimo do `.env`:**
```bash
DATABASE_PASSWORD=postgres
JWT_SECRET=<resultado do comando acima>
```

### 4️⃣ Compilar e Executar

```bash
# Instalar dependências e compilar
mvn clean install

# Executar aplicação
mvn exec:java
```

**Output esperado:**
```
========================================
  Taskly API - Inicializando...
  Ambiente: development
  Porta: 7070
========================================
🚀 Iniciando migrations...
✅ Migrations executadas: 3
✅ Servidor iniciado com sucesso!
🚀 http://localhost:7070
========================================
```

### 5️⃣ Testar API

```bash
# Health check
curl http://localhost:7070/health
```

**Resposta esperada:**
```json
{
  "status": "UP",
  "version": "1.0.0",
  "environment": "development"
}
```

---

## 📝 Testando Autenticação

### 1. Registrar Usuário

```bash
curl -X POST http://localhost:7070/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "admin@taskly.com",
    "password": "Admin123456"
  }'
```

**✅ Resposta (201 Created):**
```json
{
  "message": "Usuário cadastrado com sucesso"
}
```

**❌ Erro - Senha fraca:**
```json
{
  "error": "Validação falhou",
  "message": "Senha deve conter pelo menos uma letra maiúscula"
}
```

### 2. Fazer Login

```bash
curl -X POST http://localhost:7070/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "admin@taskly.com",
    "password": "Admin123456"
  }'
```

**✅ Resposta (200 OK):**
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJ0YXNrbHktYXBpIiwic3ViIjoiMSIsImVtYWlsIjoiYWRtaW5AdGFza2x5LmNvbSIsInJvbGVzIjpbIlVTRVIiXSwiaWF0IjoxNzA0MTEwNDAwLCJleHAiOjE3MDQxMTQwMDB9.signature",
  "refreshToken": "Kx7dP3mN9qR2sT5vW8yZ1aB4cE6fG...",
  "expiresIn": 3600000,
  "tokenType": "Bearer"
}
```

**💡 Dica:** Salve o `accessToken` em uma variável:

```bash
export TOKEN="eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
```

---

## ✅ Testando Todos

### 1. Criar Todo

```bash
curl -X POST http://localhost:7070/api/v1/todos \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Implementar testes unitários"
  }'
```

**✅ Resposta (201 Created):**
```json
{
  "id": 1,
  "title": "Implementar testes unitários",
  "done": false,
  "createdAt": "2024-01-01T10:00:00",
  "updatedAt": "2024-01-01T10:00:00"
}
```

### 2. Listar Todos

```bash
curl -X GET http://localhost:7070/api/v1/todos \
  -H "Authorization: Bearer $TOKEN"
```

**✅ Resposta (200 OK):**
```json
[
  {
    "id": 1,
    "title": "Implementar testes unitários",
    "done": false,
    "createdAt": "2024-01-01T10:00:00",
    "updatedAt": "2024-01-01T10:00:00"
  }
]
```

### 3. Alternar Status (Toggle)

```bash
curl -X PATCH http://localhost:7070/api/v1/todos/1/toggle \
  -H "Authorization: Bearer $TOKEN"
```

**✅ Resposta (200 OK):**
```json
{
  "id": 1,
  "title": "Implementar testes unitários",
  "done": true,
  "createdAt": "2024-01-01T10:00:00",
  "updatedAt": "2024-01-01T11:00:00"
}
```

### 4. Atualizar Título

```bash
curl -X PATCH http://localhost:7070/api/v1/todos/1 \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Implementar testes unitários e de integração"
  }'
```

### 5. Deletar Todo

```bash
curl -X DELETE http://localhost:7070/api/v1/todos/1 \
  -H "Authorization: Bearer $TOKEN"
```

**✅ Resposta (204 No Content)**

---

## 🔄 Testando Refresh Token

### 1. Renovar Access Token

```bash
# Salvar refresh token
export REFRESH_TOKEN="Kx7dP3mN9qR2sT5vW8yZ1aB4cE6fG..."

# Renovar
curl -X POST http://localhost:7070/api/v1/auth/refresh \
  -H "Content-Type: application/json" \
  -d "{
    \"refreshToken\": \"$REFRESH_TOKEN\"
  }"
```

**✅ Resposta (200 OK):**
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refreshToken": "nEwT0k3nG3n3r4t3dH3r3...",
  "expiresIn": 3600000,
  "tokenType": "Bearer"
}
```

**💡 Nota:** O refresh token antigo é revogado e um novo é gerado (rotação).

### 2. Fazer Logout

```bash
curl -X POST http://localhost:7070/api/v1/auth/logout \
  -H "Authorization: Bearer $TOKEN"
```

**✅ Resposta (200 OK):**
```json
{
  "message": "Logout realizado com sucesso"
}
```

**💡 Nota:** Todos os refresh tokens do usuário são revogados.

---

## 🔒 Testando Segurança

### 1. Requisição sem Token

```bash
curl -X GET http://localhost:7070/api/v1/todos
```

**❌ Resposta (401 Unauthorized):**
```json
{
  "error": "Token de autenticação não fornecido"
}
```

### 2. Token Inválido

```bash
curl -X GET http://localhost:7070/api/v1/todos \
  -H "Authorization: Bearer token_invalido"
```

**❌ Resposta (401 Unauthorized):**
```json
{
  "error": "Token inválido ou expirado"
}
```

### 3. Rate Limiting

```bash
# Fazer 6 requisições rápidas (limite é 5/min para auth)
for i in {1..6}; do
  curl -X POST http://localhost:7070/api/v1/auth/login \
    -H "Content-Type: application/json" \
    -d '{"email":"test@test.com","password":"wrong"}'
  echo ""
done
```

**Na 6ª requisição (429 Too Many Requests):**
```json
{
  "error": "Muitas requisições. Tente novamente em alguns segundos.",
  "retryAfter": 60
}
```

**Headers da resposta:**
```
X-RateLimit-Limit: 5
X-RateLimit-Remaining: 0
X-RateLimit-Reset: 1704110460
```

---

## 🧪 Script de Teste Completo

Salve este script como `test_api.sh`:

```bash
#!/bin/bash

BASE_URL="http://localhost:7070"
EMAIL="test@example.com"
PASSWORD="Test123456"

echo "🧪 Testando Taskly API..."
echo ""

# 1. Health check
echo "1️⃣ Health Check"
curl -s $BASE_URL/health | jq
echo ""

# 2. Registrar
echo "2️⃣ Registrando usuário..."
curl -s -X POST $BASE_URL/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d "{\"email\":\"$EMAIL\",\"password\":\"$PASSWORD\"}" | jq
echo ""

# 3. Login
echo "3️⃣ Fazendo login..."
RESPONSE=$(curl -s -X POST $BASE_URL/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d "{\"email\":\"$EMAIL\",\"password\":\"$PASSWORD\"}")

echo $RESPONSE | jq
TOKEN=$(echo $RESPONSE | jq -r .accessToken)
echo ""

# 4. Criar todo
echo "4️⃣ Criando todo..."
curl -s -X POST $BASE_URL/api/v1/todos \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"title":"Testar API"}' | jq
echo ""

# 5. Listar todos
echo "5️⃣ Listando todos..."
curl -s -X GET $BASE_URL/api/v1/todos \
  -H "Authorization: Bearer $TOKEN" | jq
echo ""

# 6. Toggle
echo "6️⃣ Alternando status..."
curl -s -X PATCH $BASE_URL/api/v1/todos/1/toggle \
  -H "Authorization: Bearer $TOKEN" | jq
echo ""

# 7. Logout
echo "7️⃣ Fazendo logout..."
curl -s -X POST $BASE_URL/api/v1/auth/logout \
  -H "Authorization: Bearer $TOKEN" | jq
echo ""

echo "✅ Testes concluídos!"
```

**Executar:**
```bash
chmod +x test_api.sh
./test_api.sh
```

---

## 🐘 Verificar Banco de Dados

```bash
# Conectar ao banco
psql -U postgres -d taskly

# Ver tabelas criadas
\dt

# Ver usuários
SELECT id, email, roles, active, created_at FROM users;

# Ver todos
SELECT id, user_id, title, done, created_at FROM todos;

# Ver refresh tokens
SELECT id, user_id, expires_at, revoked FROM refresh_tokens;

# Sair
\q
```

---

## 📊 Monitoramento

### Logs em Tempo Real

```bash
# Executar em background
mvn exec:java &

# Ver logs
tail -f nohup.out
```

### Verificar Audit Logs

Os logs de auditoria aparecem com `[AUDIT]`:

```
[AUDIT] AUTH_SUCCESS | email=admin@taskly.com | ip=127.0.0.1
[AUDIT] AUTH_FAILURE | email=wrong@test.com | ip=127.0.0.1 | reason=invalid_password
[AUDIT] TOKEN_REFRESHED | userId=1 | ip=127.0.0.1
```

---

## ❓ Troubleshooting

### Erro: "Connection refused"
```bash
# Verificar se PostgreSQL está rodando
pg_isready

# Iniciar PostgreSQL (macOS)
brew services start postgresql

# Iniciar PostgreSQL (Linux)
sudo systemctl start postgresql
```

### Erro: "Database does not exist"
```bash
# Criar banco
createdb taskly
```

### Erro: "JWT_SECRET não configurada"
```bash
# Gerar e adicionar ao .env
echo "JWT_SECRET=$(openssl rand -base64 32)" >> .env
```

### Porta 7070 já em uso
```bash
# Alterar porta no .env
echo "SERVER_PORT=8080" >> .env
```

---

## 🎓 Próximos Passos

1. ✅ API funcionando localmente
2. 📖 Ler [API.md](./API.md) para documentação completa
3. 🔒 Ler [SECURITY.md](./SECURITY.md) para entender segurança
4. 🏗️ Ler [ARCHITECTURE.md](./ARCHITECTURE.md) para entender arquitetura
5. 🚀 Ler [PRODUCTION_CHECKLIST.md](./PRODUCTION_CHECKLIST.md) antes de deploy

---

**🎉 Parabéns! Sua API está rodando com segurança enterprise-grade!**
