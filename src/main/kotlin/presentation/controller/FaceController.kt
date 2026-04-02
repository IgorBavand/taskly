package app.presentation.controller

import app.application.service.FaceValidationService
import app.presentation.dto.*
import app.security.userId
import io.javalin.Javalin
import io.javalin.http.Context

/**
 * Controller para endpoints de validação facial.
 *
 * Endpoints:
 * - POST /api/v1/face/register - Registra rosto do usuário
 * - POST /api/v1/face/verify - Verifica rosto e emite token
 * - GET /api/v1/face/status - Verifica se usuário tem token válido
 * - DELETE /api/v1/face - Remove rosto do usuário
 */
class FaceController(
    private val faceValidationService: FaceValidationService
) {

    fun register(app: Javalin) {
        app.post("/api/v1/face/register", ::handleRegisterFace)
        app.post("/api/v1/face/verify", ::handleVerifyFace)
        app.get("/api/v1/face/status", ::handleStatus)
        app.delete("/api/v1/face", ::handleDeleteFace)
    }

    private fun handleRegisterFace(ctx: Context) {
        val userId = ctx.userId()
        val request = ctx.bodyAsClass(FaceRegistrationRequest::class.java)

        faceValidationService.registerUserFace(userId, request.image)

        ctx.status(201).json(mapOf(
            "success" to true,
            "message" to "Rosto registrado com sucesso"
        ))
    }

    private fun handleVerifyFace(ctx: Context) {
        val userId = ctx.userId()
        val request = ctx.bodyAsClass(FaceVerificationRequest::class.java)

        val token = faceValidationService.verifyFaceAndIssueToken(userId, request.image)

        ctx.json(FaceVerificationResponse(
            success = true,
            token = token.token,
            expiresAt = token.expiresAt.toString(),
            message = "Rosto verificado com sucesso"
        ))
    }

    private fun handleStatus(ctx: Context) {
        val userId = ctx.userId()
        val (hasValidToken, expiresAt) = faceValidationService.hasValidToken(userId)

        ctx.json(FaceValidationStatusResponse(
            hasValidToken = hasValidToken,
            expiresAt = expiresAt
        ))
    }

    private fun handleDeleteFace(ctx: Context) {
        val userId = ctx.userId()

        faceValidationService.deleteUserFace(userId)

        ctx.status(204)
    }
}
