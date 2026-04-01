# Global Exception Handler - Taskly API

## 📋 Resumo da Refatoração

Implementação de um **Global Exception Handler** centralizado, eliminando blocos try/catch repetitivos nos controllers e padronizando o tratamento de erros em toda a aplicação.

---

## ✅ O Que Foi Implementado

### 1. **Domain Exceptions** (Camada de Domínio)

Criadas exceptions específicas de domínio seguindo DDD:

```kotlin
// Hierarquia de exceções
DomainException (abstract)
  ├── NotFoundException (404)
  │   ├── TodoNotFoundException
  │   └── UserNotFoundException
  ├── AccessDeniedException (403)
  │   └── TodoAccessDeniedException
  ├── ValidationException (400)
  │   ├── InvalidPasswordException
  │   ├── InvalidEmailException
  │   ├── EmailAlreadyExistsException
  │   └── InvalidDataException
  └── AuthenticationException (401)
      ├── InvalidCredentialsException
      ├── InvalidRefreshTokenException
      ├── InvalidTokenException
      └── AccountDisabledException
```

**Arquivo:** `domain/exception/DomainExceptions.kt`

### 2. **GlobalExceptionHandler** (Camada de Apresentação)

Handler centralizado que trata todas as exceptions da aplicação:

```kotlin
object GlobalExceptionHandler {
    fun register(app: Javalin) {
        // 404 - Not Found
        app.exception(NotFoundException::class.java) { e, ctx ->
            handleException(ctx, e, 404, "Recurso não encontrado")
        }

        // 403 - Forbidden
        app.exception(AccessDeniedException::class.java) { e, ctx ->
            handleException(ctx, e, 403, "Acesso negado")
        }

        // 401 - Unauthorized
        app.exception(AuthenticationException::class.java) { e, ctx ->
            handleException(ctx, e, 401, "Falha na autenticação")
        }

        // 400 - Bad Request
        app.exception(ValidationException::class.java) { e, ctx ->
            handleException(ctx, e, 400, "Erro de validação")
        }

        app.exception(IllegalArgumentException::class.java) { e, ctx ->
            handleException(ctx, e, 400, "Argumento inválido")
        }

        app.exception(NumberFormatException::class.java) { e, ctx ->
            handleException(ctx, e, 400, "Formato numérico inválido")
        }

        // 500 - Internal Server Error
        app.exception(Exception::class.java) { e, ctx ->
            handleInternalError(ctx, e)
        }

        // HTTP errors
        app.error(404) { ctx ->
            handleHttpError(ctx, 404, "Endpoint não encontrado")
        }

        app.error(405) { ctx ->
            handleHttpError(ctx, 405, "Método HTTP não permitido")
        }
    }
}
```

**Arquivo:** `presentation/exception/GlobalExceptionHandler.kt`

**Recursos:**
- ✅ Logging apropriado (warn para 4xx, error para 5xx)
- ✅ Correlação via `requestId`
- ✅ Respostas padronizadas
- ✅ Proteção em produção (não vaza stack traces)
- ✅ Tratamento de HTTP errors (404, 405, 500)

### 3. **Controllers Refatorados**

#### Antes (AuthController):
```kotlin
private fun handleRegister(ctx: Context) {
    try {
        val request = ctx.bodyAsClass(RegisterRequest::class.java)
        authService.register(
            email = request.email,
            password = request.password,
            ip = ctx.ip()
        )
        ctx.status(201).json(MessageResponse("Usuário cadastrado com sucesso"))
    } catch (e: IllegalArgumentException) {
        logger.warn("Falha no registro: {}", e.message)
        ctx.status(400).json(ErrorResponse(
            error = "Validação falhou",
            message = e.message
        ))
    }
}
```

#### Depois:
```kotlin
private fun handleRegister(ctx: Context) {
    val request = ctx.bodyAsClass(RegisterRequest::class.java)

    authService.register(
        email = request.email,
        password = request.password,
        ip = ctx.ip()
    )

    ctx.status(201).json(MessageResponse("Usuário cadastrado com sucesso"))
}
```

**Redução de código:**
- AuthController: ~100 linhas → ~70 linhas (**-30%**)
- TodoController: ~180 linhas → ~70 linhas (**-60%**)

### 4. **Services Atualizados**

Services agora lançam exceptions de domínio específicas:

```kotlin
// AuthService
fun register(email: String, password: String, ip: String): User {
    val emailVO = Email.of(email)

    if (userRepository.existsByEmail(emailVO)) {
        throw EmailAlreadyExistsException(email)  // ✅ Exception específica
    }

    val passwordHash = PasswordHasher.hash(password)  // ✅ Pode lançar InvalidPasswordException
    // ...
}

fun login(email: String, password: String, ip: String): AuthTokens {
    val user = userRepository.findByEmail(emailVO)
        ?: throw InvalidCredentialsException()  // ✅

    if (!user.active) {
        throw AccountDisabledException()  // ✅ Exception específica ao invés de InvalidCredentialsException
    }

    if (!PasswordHasher.verify(password, user.passwordHash)) {
        throw InvalidCredentialsException()
    }
    // ...
}
```

### 5. **PasswordHasher Atualizado**

Validação de senha agora lança exceptions específicas:

```kotlin
private fun validatePassword(password: String) {
    when {
        password.length < 8 ->
            throw InvalidPasswordException("Senha deve ter no mínimo 8 caracteres")
        password.length > 128 ->
            throw InvalidPasswordException("Senha muito longa (máximo 128 caracteres)")
        !password.any { it.isUpperCase() } ->
            throw InvalidPasswordException("Senha deve conter pelo menos uma letra maiúscula")
        !password.any { it.isLowerCase() } ->
            throw InvalidPasswordException("Senha deve conter pelo menos uma letra minúscula")
        !password.any { it.isDigit() } ->
            throw InvalidPasswordException("Senha deve conter pelo menos um número")
    }
}
```

---

## 📊 Comparação: Antes vs Depois

### Controllers

| Aspecto | Antes | Depois |
|---------|-------|--------|
| Try/catch blocks | 10+ blocos | 0 blocos |
| Linhas de código | ~280 linhas | ~140 linhas |
| Responsabilidade | Lógica + Error handling | Apenas lógica |
| Código duplicado | Alto | Nenhum |
| Manutenibilidade | Baixa | Alta |

### Exception Handling

| Aspecto | Antes | Depois |
|---------|-------|--------|
| Local | Controllers | GlobalExceptionHandler |
| Padronização | Inconsistente | Totalmente padronizado |
| Logging | Duplicado | Centralizado |
| RequestId | Não utilizado | Incluído em todos os logs |
| Produção-ready | Parcial | Completo |

---

## 🎯 Benefícios da Refatoração

### 1. **Código Mais Limpo**
- Controllers focam apenas na lógica de apresentação
- Sem blocos try/catch repetitivos
- Redução de ~50% no código dos controllers

### 2. **Manutenibilidade**
- Um único ponto para alterar tratamento de erros
- Fácil adicionar novas exceptions
- Consistência garantida

### 3. **Padronização**
- Todas as respostas de erro seguem o mesmo formato
- Logging consistente em toda aplicação
- Correlação via requestId

### 4. **Observabilidade**
- Logs estruturados com nível apropriado (warn/error)
- RequestId em todos os logs
- Stack trace completo para erros 5xx

### 5. **Segurança**
- Stack traces não vazam em produção
- Mensagens de erro apropriadas
- Logging de tentativas de acesso não autorizado

### 6. **Escalabilidade**
- Fácil adicionar novos tipos de exceptions
- Hierarquia extensível
- Separação clara de responsabilidades

---

## 🔧 Como Usar

### Lançar uma Exception no Service

```kotlin
class TodoService(private val todoRepository: TodoRepository) {

    fun getById(id: Long, userId: Long): Todo {
        val todo = todoRepository.findById(id, userId)
            ?: throw TodoNotFoundException()  // ✅ Simples!

        if (!todo.belongsTo(userId)) {
            throw TodoAccessDeniedException()  // ✅
        }

        return todo
    }
}
```

### No Controller (Nenhum Try/Catch!)

```kotlin
class TodoController(private val todoService: TodoService) {

    private fun handleGetTodo(ctx: Context) {
        val userId = ctx.userId()
        val id = ctx.pathParam("id").toLong()
        val todo = todoService.getById(id, userId)  // ✅ Pode lançar exceptions

        ctx.json(TodoResponse.fromEntity(todo))
    }
}
```

### GlobalExceptionHandler Cuida do Resto

```
TodoService.getById()
    └── throw TodoNotFoundException()
        └── GlobalExceptionHandler captura
            ├── Log: WARN "Client error [404] GET /api/v1/todos/123 - Todo não encontrado"
            └── Response: { "error": "Todo não encontrado", "path": "/api/v1/todos/123" }
```

---

## 📝 Formato de Resposta Padronizado

### Sucesso
```json
// 200 OK
{
  "id": 1,
  "title": "Minha tarefa",
  "done": false,
  "createdAt": "2024-01-01T10:00:00",
  "updatedAt": "2024-01-01T10:00:00"
}
```

### Erro
```json
// 404 Not Found
{
  "error": "Todo não encontrado",
  "message": null,
  "timestamp": "2024-01-01T12:00:00",
  "path": "/api/v1/todos/999"
}

// 400 Bad Request
{
  "error": "Senha inválida",
  "message": "Senha deve conter pelo menos uma letra maiúscula",
  "timestamp": "2024-01-01T12:00:00",
  "path": "/api/v1/auth/register"
}

// 401 Unauthorized
{
  "error": "Credenciais inválidas",
  "message": null,
  "timestamp": "2024-01-01T12:00:00",
  "path": "/api/v1/auth/login"
}

// 403 Forbidden
{
  "error": "Acesso negado a este recurso",
  "message": null,
  "timestamp": "2024-01-01T12:00:00",
  "path": "/api/v1/todos/5"
}

// 500 Internal Server Error (Development)
{
  "error": "Erro interno do servidor",
  "message": "NullPointerException: field x is null",
  "timestamp": "2024-01-01T12:00:00",
  "path": "/api/v1/todos"
}

// 500 Internal Server Error (Production)
{
  "error": "Erro interno do servidor",
  "message": "Ocorreu um erro inesperado. Por favor, tente novamente.",
  "timestamp": "2024-01-01T12:00:00",
  "path": "/api/v1/todos"
}
```

---

## 📈 Logs Estruturados

### Client Errors (4xx) - WARN
```
[WARN] Client error [400] POST /api/v1/auth/register - Senha inválida | requestId=123e4567-e89b
[WARN] Client error [401] POST /api/v1/auth/login - Credenciais inválidas | requestId=123e4567-e89b
[WARN] Client error [403] GET /api/v1/todos/5 - Acesso negado | requestId=123e4567-e89b
[WARN] Client error [404] GET /api/v1/todos/999 - Todo não encontrado | requestId=123e4567-e89b
```

### Server Errors (5xx) - ERROR (com stack trace)
```
[ERROR] Server error [500] GET /api/v1/todos | requestId=123e4567-e89b
java.lang.NullPointerException: Cannot invoke method on null object
    at app.application.service.TodoService.listAll(TodoService.kt:21)
    at app.presentation.controller.TodoController.handleListTodos(TodoController.kt:30)
    ...
```

### HTTP Errors - WARN
```
[WARN] HTTP error [404] GET /api/v2/invalid-endpoint | requestId=123e4567-e89b
[WARN] HTTP error [405] PUT /api/v1/todos | requestId=123e4567-e89b
```

---

## 🧩 Adicionar Nova Exception

### 1. Criar Exception no Domain

```kotlin
// domain/exception/DomainExceptions.kt

class TaskLimitExceededException(
    limit: Int
) : ValidationException("Limite de tarefas atingido: $limit")
```

### 2. Registrar no GlobalExceptionHandler

```kotlin
// presentation/exception/GlobalExceptionHandler.kt

app.exception(TaskLimitExceededException::class.java) { e, ctx ->
    handleException(ctx, e, 400, "Limite de tarefas excedido")
}
```

### 3. Usar no Service

```kotlin
fun create(title: String, userId: Long): Todo {
    val userTodoCount = todoRepository.countByUserId(userId)

    if (userTodoCount >= 100) {
        throw TaskLimitExceededException(100)
    }

    // ...
}
```

**Pronto!** Nenhuma alteração nos controllers necessária.

---

## 🎓 Princípios Aplicados

### 1. **DRY (Don't Repeat Yourself)**
- Tratamento de erro centralizado
- Logging sem duplicação
- Formato de resposta único

### 2. **Single Responsibility Principle (SOLID)**
- Controllers: apenas apresentação
- Services: apenas lógica de negócio
- GlobalExceptionHandler: apenas tratamento de erros

### 3. **Open/Closed Principle (SOLID)**
- Fácil estender com novas exceptions
- Sem modificar código existente

### 4. **Separation of Concerns**
- Domain: exceções de negócio
- Application: uso das exceções
- Presentation: tratamento centralizado

### 5. **Domain-Driven Design**
- Exceptions específicas de domínio
- Linguagem ubíqua (TodoNotFoundException, EmailAlreadyExistsException)
- Camadas bem definidas

---

## ✅ Checklist de Implementação

- [x] Criar hierarquia de domain exceptions
- [x] Criar GlobalExceptionHandler
- [x] Refatorar AuthService (usar domain exceptions)
- [x] Refatorar TodoService (usar domain exceptions)
- [x] Atualizar PasswordHasher (lançar InvalidPasswordException)
- [x] Refatorar AuthController (remover try/catch)
- [x] Refatorar TodoController (remover try/catch)
- [x] Registrar GlobalExceptionHandler no Main.kt
- [x] Testar compilação
- [x] Documentar refatoração

---

## 📚 Arquivos Modificados/Criados

### Criados
1. `domain/exception/DomainExceptions.kt` - Hierarquia de exceptions
2. `presentation/exception/GlobalExceptionHandler.kt` - Handler centralizado
3. `GLOBAL_EXCEPTION_HANDLER.md` - Esta documentação

### Modificados
1. `application/service/AuthService.kt` - Usa domain exceptions
2. `application/service/TodoService.kt` - Usa domain exceptions
3. `security/PasswordHasher.kt` - Lança InvalidPasswordException
4. `presentation/controller/AuthController.kt` - Removidos try/catch
5. `presentation/controller/TodoController.kt` - Removidos try/catch
6. `config/JavalinConfig.kt` - Removido exception handler local
7. `Main.kt` - Registra GlobalExceptionHandler

---

## 🚀 Próximos Passos (Opcional)

### 1. Exception Mapping Personalizado
```kotlin
// Para casos mais complexos
data class ValidationErrorResponse(
    val error: String,
    val violations: List<FieldViolation>
)

data class FieldViolation(
    val field: String,
    val message: String
)
```

### 2. Metrics
```kotlin
// Contar exceptions por tipo
private val exceptionMetrics = mutableMapOf<String, AtomicInteger>()

private fun handleException(...) {
    exceptionMetrics
        .computeIfAbsent(exception::class.simpleName) { AtomicInteger() }
        .incrementAndGet()
    // ...
}
```

### 3. Alerting
```kotlin
// Alertar em exceptions críticas
if (statusCode >= 500) {
    alertingService.sendAlert(
        "Critical error: ${exception.message}",
        severity = Severity.HIGH
    )
}
```

---

## 💡 Conclusão

A refatoração para um **Global Exception Handler** trouxe:

✅ **Código 50% mais limpo** nos controllers
✅ **Manutenibilidade** drasticamente melhorada
✅ **Padronização** completa de respostas
✅ **Observabilidade** com logs estruturados
✅ **Escalabilidade** para novos tipos de erros
✅ **Segurança** adequada para produção

**Resultado:** API mais profissional, robusta e fácil de manter! 🎉
