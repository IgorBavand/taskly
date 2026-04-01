package app.presentation.exception

import app.domain.exception.*
import app.presentation.dto.ErrorResponse
import app.config.EnvConfig
import io.javalin.Javalin
import io.javalin.http.Context
import org.slf4j.LoggerFactory

/**
 * Global Exception Handler para a aplicação.
 *
 * Centraliza o tratamento de todas as exceções, garantindo:
 * - Respostas padronizadas
 * - Logging apropriado
 * - Correlação via requestId
 * - Segurança (não vazar stack traces em produção)
 */
object GlobalExceptionHandler {

    private val logger = LoggerFactory.getLogger(GlobalExceptionHandler::class.java)

    /**
     * Registra todos os exception handlers no Javalin
     */
    fun register(app: Javalin) {
        // Domain Exceptions (ordem específica importa - mais específica primeiro)

        // 404 - Not Found
        app.exception(NotFoundException::class.java) { e, ctx ->
            handleException(ctx, e, 404, "Recurso não encontrado")
        }

        app.exception(TodoNotFoundException::class.java) { e, ctx ->
            handleException(ctx, e, 404, "Todo não encontrado")
        }

        app.exception(UserNotFoundException::class.java) { e, ctx ->
            handleException(ctx, e, 404, "Usuário não encontrado")
        }

        // 403 - Forbidden
        app.exception(AccessDeniedException::class.java) { e, ctx ->
            handleException(ctx, e, 403, "Acesso negado")
        }

        app.exception(TodoAccessDeniedException::class.java) { e, ctx ->
            handleException(ctx, e, 403, "Acesso negado a este recurso")
        }

        app.exception(InsufficientPermissionsException::class.java) { e, ctx ->
            handleException(ctx, e, 403, "Permissão insuficiente")
        }

        // 401 - Unauthorized
        app.exception(AuthenticationException::class.java) { e, ctx ->
            handleException(ctx, e, 401, "Falha na autenticação")
        }

        app.exception(InvalidCredentialsException::class.java) { e, ctx ->
            handleException(ctx, e, 401, "Credenciais inválidas")
        }

        app.exception(InvalidRefreshTokenException::class.java) { e, ctx ->
            handleException(ctx, e, 401, "Refresh token inválido")
        }

        app.exception(InvalidTokenException::class.java) { e, ctx ->
            handleException(ctx, e, 401, "Token inválido ou expirado")
        }

        app.exception(AccountDisabledException::class.java) { e, ctx ->
            handleException(ctx, e, 401, "Conta desativada")
        }

        // 400 - Bad Request (Validations)
        app.exception(ValidationException::class.java) { e, ctx ->
            handleException(ctx, e, 400, "Erro de validação")
        }

        app.exception(InvalidPasswordException::class.java) { e, ctx ->
            handleException(ctx, e, 400, "Senha inválida")
        }

        app.exception(InvalidEmailException::class.java) { e, ctx ->
            handleException(ctx, e, 400, "Email inválido")
        }

        app.exception(EmailAlreadyExistsException::class.java) { e, ctx ->
            handleException(ctx, e, 400, "Email já cadastrado")
        }

        app.exception(InvalidDataException::class.java) { e, ctx ->
            handleException(ctx, e, 400, "Dados inválidos")
        }

        // Built-in exceptions
        app.exception(IllegalArgumentException::class.java) { e, ctx ->
            handleException(ctx, e, 400, "Argumento inválido")
        }

        app.exception(NumberFormatException::class.java) { e, ctx ->
            handleException(ctx, e, 400, "Formato numérico inválido")
        }

        // 500 - Internal Server Error (fallback)
        app.exception(Exception::class.java) { e, ctx ->
            handleInternalError(ctx, e)
        }

        // HTTP Error handlers (para erros não capturados por exceptions)
        app.error(404) { ctx ->
            handleHttpError(ctx, 404, "Endpoint não encontrado")
        }

        app.error(405) { ctx ->
            handleHttpError(ctx, 405, "Método HTTP não permitido")
        }

        app.error(500) { ctx ->
            handleHttpError(ctx, 500, "Erro interno do servidor")
        }
    }

    /**
     * Trata exceções de domínio e conhecidas
     */
    private fun handleException(
        ctx: Context,
        exception: Exception,
        statusCode: Int,
        defaultError: String
    ) {
        val requestId = ctx.attribute<String>("requestId") ?: "unknown"
        val path = ctx.path()
        val method = ctx.method()

        // Log apropriado baseado no status code
        when (statusCode) {
            in 400..499 -> {
                // Client errors - log como warning
                logger.warn(
                    "Client error [{}] {} {} - {} | requestId={}",
                    statusCode, method, path, exception.message, requestId
                )
            }
            in 500..599 -> {
                // Server errors - log como error com stack trace
                logger.error(
                    "Server error [{}] {} {} | requestId={}",
                    statusCode, method, path, requestId, exception
                )
            }
            else -> {
                logger.info(
                    "Exception [{}] {} {} - {} | requestId={}",
                    statusCode, method, path, exception.message, requestId
                )
            }
        }

        // Resposta padronizada
        val response = ErrorResponse(
            error = defaultError,
            message = exception.message,
            path = path
        )

        ctx.status(statusCode).json(response)
    }

    /**
     * Trata erros internos (500)
     */
    private fun handleInternalError(ctx: Context, exception: Exception) {
        val requestId = ctx.attribute<String>("requestId") ?: "unknown"
        val path = ctx.path()
        val method = ctx.method()

        // Log completo com stack trace
        logger.error(
            "Internal server error {} {} | requestId={}",
            method, path, requestId, exception
        )

        // Resposta segura (não expõe detalhes internos em produção)
        val response = if (EnvConfig.isDevelopment()) {
            ErrorResponse(
                error = "Erro interno do servidor",
                message = exception.message,
                path = path
            )
        } else {
            ErrorResponse(
                error = "Erro interno do servidor",
                message = "Ocorreu um erro inesperado. Por favor, tente novamente.",
                path = path
            )
        }

        ctx.status(500).json(response)
    }

    /**
     * Trata erros HTTP genéricos (404, 405, etc)
     */
    private fun handleHttpError(ctx: Context, statusCode: Int, message: String) {
        val requestId = ctx.attribute<String>("requestId") ?: "unknown"
        val path = ctx.path()
        val method = ctx.method()

        logger.warn(
            "HTTP error [{}] {} {} | requestId={}",
            statusCode, method, path, requestId
        )

        val response = ErrorResponse(
            error = message,
            message = null,
            path = path
        )

        ctx.status(statusCode).json(response)
    }
}
