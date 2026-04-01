package app.presentation.controller

import app.application.service.AuthService
import app.presentation.dto.*
import app.security.AuditLogger
import io.javalin.Javalin
import io.javalin.http.Context

/**
 * Controller de autenticação (Presentation Layer - DDD)
 *
 * Todas as exceptions são tratadas pelo GlobalExceptionHandler.
 * Não há try/catch aqui - apenas lógica de negócio limpa.
 */
class AuthController(private val authService: AuthService) {

    fun register(app: Javalin) {
        app.post("/api/v1/auth/register", ::handleRegister)
        app.post("/api/v1/auth/login", ::handleLogin)
        app.post("/api/v1/auth/refresh", ::handleRefreshToken)
        app.post("/api/v1/auth/logout", ::handleLogout)
    }

    private fun handleRegister(ctx: Context) {
        val request = ctx.bodyAsClass(RegisterRequest::class.java)

        authService.register(
            email = request.email,
            password = request.password,
            ip = ctx.ip()
        )

        ctx.status(201).json(MessageResponse("Usuário cadastrado com sucesso"))
    }

    private fun handleLogin(ctx: Context) {
        val request = ctx.bodyAsClass(LoginRequest::class.java)

        val tokens = authService.login(
            email = request.email,
            password = request.password,
            ip = ctx.ip()
        )

        ctx.json(AuthResponse(
            accessToken = tokens.accessToken,
            refreshToken = tokens.refreshToken,
            expiresIn = tokens.expiresIn
        ))
    }

    private fun handleRefreshToken(ctx: Context) {
        val request = ctx.bodyAsClass(RefreshTokenRequest::class.java)

        val tokens = authService.refreshToken(
            refreshTokenString = request.refreshToken,
            ip = ctx.ip()
        )

        ctx.json(AuthResponse(
            accessToken = tokens.accessToken,
            refreshToken = tokens.refreshToken,
            expiresIn = tokens.expiresIn
        ))
    }

    private fun handleLogout(ctx: Context) {
        // Requer autenticação (middleware já validou)
        val userId = ctx.attribute<Long>("userId")!!

        authService.logout(userId)

        AuditLogger.logSensitiveAction(userId, "LOGOUT", "/auth/logout", ctx.ip())

        ctx.json(MessageResponse("Logout realizado com sucesso"))
    }
}
