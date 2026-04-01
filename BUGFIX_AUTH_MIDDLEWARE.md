# Bugfix: AuthMiddleware NullPointerException

## 🐛 Problema Identificado

### Erro Original
```
[ERROR] Internal server error GET /api/v1/todos | requestId=b6836f87-e305-4be9-8753-603868783c29
java.lang.NullPointerException
    at app.security.AuthMiddlewareKt.userId(AuthMiddleware.kt:65)
    at app.presentation.controller.TodoController.handleListTodos(TodoController.kt:31)
```

### Causa Raiz

O **AuthMiddleware** estava retornando uma resposta de erro (401) mas **não estava impedindo a execução do próximo handler** no pipeline do Javalin.

#### Código Problemático (Antes):

```kotlin
class AuthMiddleware : Handler {
    override fun handle(ctx: Context) {
        val authHeader = ctx.header("Authorization")

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            logger.warn("Token não fornecido. Path: ${ctx.path()}, IP: ${ctx.ip()}")
            ctx.status(401).json(mapOf("error" to "Token de autenticação não fornecido"))
            return  // ❌ Não impede execução do próximo handler!
        }
        // ...
    }
}
```

#### O Que Acontecia:

1. Request `GET /api/v1/todos` sem token
2. AuthMiddleware detecta falta de token
3. AuthMiddleware retorna 401 com `return`
4. ❌ **Javalin continua executando o próximo handler** (TodoController)
5. TodoController chama `ctx.userId()` que retorna `null`
6. 💥 **NullPointerException**

---

## ✅ Solução Implementada

### Abordagem

Ao invés de retornar com status code, o middleware agora **lança exceptions** que são capturadas pelo **GlobalExceptionHandler**, interrompendo o pipeline.

### Código Corrigido (Depois):

```kotlin
class AuthMiddleware : Handler {
    override fun handle(ctx: Context) {
        val authHeader = ctx.header("Authorization")

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            logger.warn("Token não fornecido. Path: ${ctx.path()}, IP: ${ctx.ip()}")
            throw InvalidTokenException("Token de autenticação não fornecido")  // ✅
        }

        val token = authHeader.substring(7).trim()
        val claims = JwtProvider.validateToken(token)

        if (claims == null) {
            logger.warn("Token inválido ou expirado. Path: ${ctx.path()}, IP: ${ctx.ip()}")
            throw InvalidTokenException("Token inválido ou expirado")  // ✅
        }

        // Claims adicionados ao contexto
        ctx.attribute("userId", claims.userId)
        ctx.attribute("userEmail", claims.email)
        ctx.attribute("userRoles", claims.roles)
        ctx.attribute("jwtClaims", claims)
    }
}
```

### Como Funciona Agora:

1. Request `GET /api/v1/todos` sem token
2. AuthMiddleware detecta falta de token
3. AuthMiddleware **lança `InvalidTokenException`**
4. ✅ **GlobalExceptionHandler captura a exception**
5. ✅ **Pipeline é interrompido** (TodoController não é executado)
6. ✅ **Resposta 401 padronizada** é retornada

---

## 📝 Alterações Realizadas

### 1. Nova Exception de Domínio

```kotlin
// domain/exception/DomainExceptions.kt

/**
 * Permissão insuficiente (falta role necessária)
 */
class InsufficientPermissionsException(
    requiredRole: String
) : AccessDeniedException("Permissão insuficiente. Necessária role: $requiredRole")
```

### 2. AuthMiddleware Atualizado

```kotlin
// security/AuthMiddleware.kt

import app.domain.exception.InvalidTokenException
import app.domain.exception.InsufficientPermissionsException

class AuthMiddleware : Handler {
    override fun handle(ctx: Context) {
        // ... validações ...

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw InvalidTokenException("Token de autenticação não fornecido")
        }

        if (claims == null) {
            throw InvalidTokenException("Token inválido ou expirado")
        }
    }
}

class RequireRole(private val requiredRole: Role) : Handler {
    override fun handle(ctx: Context) {
        val roles = ctx.attribute<Set<Role>>("userRoles") ?: emptySet()

        if (!roles.contains(requiredRole)) {
            throw InsufficientPermissionsException(requiredRole.name)
        }
    }
}
```

### 3. GlobalExceptionHandler Atualizado

```kotlin
// presentation/exception/GlobalExceptionHandler.kt

app.exception(InsufficientPermissionsException::class.java) { e, ctx ->
    handleException(ctx, e, 403, "Permissão insuficiente")
}
```

---

## 🧪 Teste do Fix

### Cenário 1: Request sem Token

**Request:**
```bash
curl -X GET http://localhost:7070/api/v1/todos
```

**Antes (Bug):**
```json
❌ 500 Internal Server Error
{
  "error": "Erro interno do servidor",
  "message": "NullPointerException"
}
```

**Depois (Fix):**
```json
✅ 401 Unauthorized
{
  "error": "Token inválido ou expirado",
  "message": "Token de autenticação não fornecido",
  "timestamp": "2024-01-01T12:00:00",
  "path": "/api/v1/todos"
}
```

### Cenário 2: Token Inválido

**Request:**
```bash
curl -X GET http://localhost:7070/api/v1/todos \
  -H "Authorization: Bearer token_invalido"
```

**Antes (Bug):**
```json
❌ 500 Internal Server Error (NullPointerException)
```

**Depois (Fix):**
```json
✅ 401 Unauthorized
{
  "error": "Token inválido ou expirado",
  "message": "Token inválido ou expirado",
  "timestamp": "2024-01-01T12:00:00",
  "path": "/api/v1/todos"
}
```

### Cenário 3: Usuário sem Role Necessária

**Request:**
```bash
curl -X DELETE http://localhost:7070/api/v1/admin/users/123 \
  -H "Authorization: Bearer <token_user_sem_role_admin>"
```

**Antes (Bug):**
```json
❌ 500 Internal Server Error (NullPointerException)
```

**Depois (Fix):**
```json
✅ 403 Forbidden
{
  "error": "Permissão insuficiente",
  "message": "Permissão insuficiente. Necessária role: ADMIN",
  "timestamp": "2024-01-01T12:00:00",
  "path": "/api/v1/admin/users/123"
}
```

---

## 📊 Logs Estruturados

### Antes (Bug):
```
[WARN] Token não fornecido. Path: /api/v1/todos, IP: 127.0.0.1
[ERROR] Internal server error GET /api/v1/todos | requestId=uuid
java.lang.NullPointerException
    at app.security.AuthMiddlewareKt.userId(AuthMiddleware.kt:65)
    ...
```

### Depois (Fix):
```
[WARN] Token não fornecido. Path: /api/v1/todos, IP: 127.0.0.1
[WARN] Client error [401] GET /api/v1/todos - Token inválido ou expirado | requestId=uuid
```

---

## 🎯 Por Que Exceptions ao Invés de Return?

### Problema com `return` no Javalin

No Javalin, quando você usa `return` em um `Handler`, ele:
- ✅ Retorna do método atual
- ❌ **NÃO interrompe o pipeline de handlers**
- ❌ Próximo handler na cadeia continua sendo executado

### Vantagens de Usar Exceptions

1. **Interrompe Pipeline**: Exception para a execução imediatamente
2. **Tratamento Centralizado**: GlobalExceptionHandler cuida de tudo
3. **Padronização**: Mesma lógica para todos os erros
4. **Logging Consistente**: Sempre com requestId e contexto
5. **Manutenibilidade**: Um único ponto para alterar comportamento

---

## 📋 Checklist de Correção

- [x] Criar `InsufficientPermissionsException`
- [x] Atualizar `AuthMiddleware` para lançar `InvalidTokenException`
- [x] Atualizar `RequireRole` para lançar `InsufficientPermissionsException`
- [x] Registrar nova exception no `GlobalExceptionHandler`
- [x] Testar compilação
- [x] Documentar fix

---

## 🔍 Lições Aprendidas

### 1. Handler Pipeline no Javalin
- `return` não interrompe pipeline
- Use exceptions para interromper execução
- GlobalExceptionHandler captura todas as exceptions

### 2. Middleware de Segurança
- Deve sempre interromper pipeline em caso de falha
- Exceptions são preferíveis a retornos antecipados
- Logging antes de lançar exception ajuda na depuração

### 3. Design Pattern
- Exception-based flow control é apropriado para segurança
- Centralização de error handling simplifica manutenção
- Domain exceptions fornecem contexto semântico

---

## 🚀 Próximos Passos (Recomendações)

### 1. Testes Automatizados
```kotlin
@Test
fun `deve retornar 401 quando token nao fornecido`() {
    val response = client.get("/api/v1/todos")

    assertEquals(401, response.status)
    assertContains(response.body, "Token de autenticação não fornecido")
}

@Test
fun `deve retornar 403 quando role insuficiente`() {
    val response = client.delete("/api/v1/admin/users/1") {
        header("Authorization", "Bearer <token_sem_admin>")
    }

    assertEquals(403, response.status)
    assertContains(response.body, "Permissão insuficiente")
}
```

### 2. Monitoring
- Alertar em alta taxa de 401/403
- Dashboard de tentativas de acesso não autorizado
- Metrics de exceptions por tipo

### 3. Rate Limiting Específico
- Limitar tentativas com tokens inválidos
- Bloquear IPs com muitas tentativas falhadas
- Honeypot endpoints para detectar scanners

---

## ✅ Conclusão

O bug foi causado por um **mal-entendido sobre o comportamento do pipeline do Javalin**. A solução foi **migrar de `return` para exceptions**, garantindo que:

✅ Pipeline é sempre interrompido em falhas de autenticação
✅ GlobalExceptionHandler trata todos os erros de forma consistente
✅ Respostas são padronizadas
✅ Logs são estruturados e completos
✅ Código fica mais limpo e manutenível

**Status:** ✅ **RESOLVIDO**
