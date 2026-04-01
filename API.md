# Taskly API - Documentação

## Base URL
```
http://localhost:7070/api/v1
```

## Autenticação

Todas as rotas de `/todos` requerem autenticação via JWT Bearer Token.

```http
Authorization: Bearer <access_token>
```

---

## Endpoints de Autenticação

### 1. Registrar Usuário
```http
POST /api/v1/auth/register
Content-Type: application/json

{
  "email": "user@example.com",
  "password": "SecurePass123"
}
```

**Resposta (201 Created):**
```json
{
  "message": "Usuário cadastrado com sucesso"
}
```

**Validações de Senha:**
- Mínimo 8 caracteres
- Pelo menos 1 letra maiúscula
- Pelo menos 1 letra minúscula
- Pelo menos 1 número

---

### 2. Login
```http
POST /api/v1/auth/login
Content-Type: application/json

{
  "email": "user@example.com",
  "password": "SecurePass123"
}
```

**Resposta (200 OK):**
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refreshToken": "Kx7dP3mN9qR2sT5vW8yZ1aB4cE6fG",
  "expiresIn": 3600000,
  "tokenType": "Bearer"
}
```

**Erros:**
```json
// 401 Unauthorized
{
  "error": "Autenticação falhou",
  "message": "Credenciais inválidas",
  "timestamp": "2024-01-01T12:00:00"
}

// 400 Bad Request
{
  "error": "Validação falhou",
  "message": "Email inválido: user@example",
  "timestamp": "2024-01-01T12:00:00"
}
```

---

### 3. Refresh Token
```http
POST /api/v1/auth/refresh
Content-Type: application/json

{
  "refreshToken": "Kx7dP3mN9qR2sT5vW8yZ1aB4cE6fG"
}
```

**Resposta (200 OK):**
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refreshToken": "nEwT0k3nG3n3r4t3dH3r3",
  "expiresIn": 3600000,
  "tokenType": "Bearer"
}
```

**Nota:** O refresh token é rotacionado (o antigo é revogado, um novo é gerado).

---

### 4. Logout
```http
POST /api/v1/auth/logout
Authorization: Bearer <access_token>
```

**Resposta (200 OK):**
```json
{
  "message": "Logout realizado com sucesso"
}
```

**Nota:** Revoga todos os refresh tokens do usuário.

---

## Endpoints de Todos

### 5. Listar Todos
```http
GET /api/v1/todos
Authorization: Bearer <access_token>
```

**Resposta (200 OK):**
```json
[
  {
    "id": 1,
    "title": "Estudar Kotlin",
    "done": false,
    "createdAt": "2024-01-01T10:00:00",
    "updatedAt": "2024-01-01T10:00:00"
  },
  {
    "id": 2,
    "title": "Implementar segurança",
    "done": true,
    "createdAt": "2024-01-01T11:00:00",
    "updatedAt": "2024-01-01T12:00:00"
  }
]
```

---

### 6. Buscar Todo por ID
```http
GET /api/v1/todos/{id}
Authorization: Bearer <access_token>
```

**Resposta (200 OK):**
```json
{
  "id": 1,
  "title": "Estudar Kotlin",
  "done": false,
  "createdAt": "2024-01-01T10:00:00",
  "updatedAt": "2024-01-01T10:00:00"
}
```

**Erros:**
```json
// 404 Not Found
{
  "error": "Todo não encontrado",
  "timestamp": "2024-01-01T12:00:00"
}

// 403 Forbidden
{
  "error": "Você não tem permissão para acessar este todo",
  "timestamp": "2024-01-01T12:00:00"
}
```

---

### 7. Criar Todo
```http
POST /api/v1/todos
Authorization: Bearer <access_token>
Content-Type: application/json

{
  "title": "Nova tarefa"
}
```

**Resposta (201 Created):**
```json
{
  "id": 3,
  "title": "Nova tarefa",
  "done": false,
  "createdAt": "2024-01-01T13:00:00",
  "updatedAt": "2024-01-01T13:00:00"
}
```

**Validações:**
- Título obrigatório
- Máximo 500 caracteres

---

### 8. Alternar Status (Toggle)
```http
PATCH /api/v1/todos/{id}/toggle
Authorization: Bearer <access_token>
```

**Resposta (200 OK):**
```json
{
  "id": 1,
  "title": "Estudar Kotlin",
  "done": true,
  "createdAt": "2024-01-01T10:00:00",
  "updatedAt": "2024-01-01T14:00:00"
}
```

---

### 9. Atualizar Título
```http
PATCH /api/v1/todos/{id}
Authorization: Bearer <access_token>
Content-Type: application/json

{
  "title": "Estudar Kotlin avançado"
}
```

**Resposta (200 OK):**
```json
{
  "id": 1,
  "title": "Estudar Kotlin avançado",
  "done": true,
  "createdAt": "2024-01-01T10:00:00",
  "updatedAt": "2024-01-01T15:00:00"
}
```

---

### 10. Deletar Todo
```http
DELETE /api/v1/todos/{id}
Authorization: Bearer <access_token>
```

**Resposta (204 No Content)**

---

## Endpoints Utilitários

### Health Check
```http
GET /health
```

**Resposta (200 OK):**
```json
{
  "status": "UP",
  "version": "1.0.0",
  "environment": "development"
}
```

### Root
```http
GET /
```

**Resposta (200 OK):**
```json
{
  "message": "Taskly API",
  "version": "1.0.0",
  "docs": "/api/v1"
}
```

---

## Rate Limiting

A API implementa rate limiting para proteção contra abuso:

**Global:**
- 100 requisições por minuto por IP

**Autenticação (/auth/login, /auth/register):**
- 5 requisições por minuto por IP

**Headers de Resposta:**
```http
X-RateLimit-Limit: 100
X-RateLimit-Remaining: 95
X-RateLimit-Reset: 1704110460
```

**Erro (429 Too Many Requests):**
```json
{
  "error": "Muitas requisições. Tente novamente em alguns segundos.",
  "retryAfter": 60
}
```

---

## Códigos de Status HTTP

| Código | Significado |
|--------|-------------|
| 200 | OK - Requisição bem-sucedida |
| 201 | Created - Recurso criado com sucesso |
| 204 | No Content - Sucesso sem corpo de resposta |
| 400 | Bad Request - Erro de validação |
| 401 | Unauthorized - Autenticação necessária ou inválida |
| 403 | Forbidden - Permissão insuficiente |
| 404 | Not Found - Recurso não encontrado |
| 429 | Too Many Requests - Rate limit excedido |
| 500 | Internal Server Error - Erro interno |

---

## Exemplo de Fluxo Completo

```bash
# 1. Registrar usuário
curl -X POST http://localhost:7070/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{"email":"dev@example.com","password":"DevPass123"}'

# 2. Fazer login
curl -X POST http://localhost:7070/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"dev@example.com","password":"DevPass123"}'

# Salvar o accessToken retornado
export TOKEN="eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."

# 3. Criar um todo
curl -X POST http://localhost:7070/api/v1/todos \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"title":"Implementar testes"}'

# 4. Listar todos
curl -X GET http://localhost:7070/api/v1/todos \
  -H "Authorization: Bearer $TOKEN"

# 5. Alternar status
curl -X PATCH http://localhost:7070/api/v1/todos/1/toggle \
  -H "Authorization: Bearer $TOKEN"

# 6. Fazer logout
curl -X POST http://localhost:7070/api/v1/auth/logout \
  -H "Authorization: Bearer $TOKEN"
```

---

## Headers de Segurança

Todas as respostas incluem headers de segurança:

```http
X-Content-Type-Options: nosniff
X-Frame-Options: DENY
X-XSS-Protection: 1; mode=block
Referrer-Policy: strict-origin-when-cross-origin
Content-Security-Policy: default-src 'self'
X-Request-ID: <uuid>
```
