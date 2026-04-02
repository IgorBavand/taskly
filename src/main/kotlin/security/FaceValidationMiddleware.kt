package app.security

import app.application.service.FaceValidationService
import app.domain.exception.AccessDeniedException
import io.javalin.http.Context
import io.javalin.http.Handler

/**
 * Middleware para validação facial obrigatória.
 *
 * Verifica se o usuário possui um token de validação facial válido.
 * Deve ser aplicado APENAS em rotas que exigem validação facial (ex: POST /todos).
 *
 * Header esperado:
 * X-Face-Token: {token}
 */
class FaceValidationMiddleware(
    private val faceValidationService: FaceValidationService
) : Handler {

    override fun handle(ctx: Context) {
        // Aplicar validação facial apenas em POST
        if (ctx.method().toString() != "POST") {
            return
        }

        val userId = ctx.userId()
        val faceToken = ctx.header("X-Face-Token")

        if (faceToken.isNullOrBlank()) {
            throw AccessDeniedException("Validação facial obrigatória para criar tarefas")
        }

        val isValid = faceValidationService.validateToken(faceToken, userId)

        if (!isValid) {
            throw AccessDeniedException("Token de validação facial inválido ou expirado")
        }

        // Token válido - consumir para evitar reuso
        faceValidationService.consumeToken(faceToken)
    }
}

/**
 * Extension para obter o Face Token do Context
 */
fun Context.faceToken(): String? = header("X-Face-Token")
