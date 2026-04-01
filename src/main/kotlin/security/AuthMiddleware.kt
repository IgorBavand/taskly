package app.security

import io.javalin.http.Context
import io.javalin.http.Handler
import app.domain.entity.Role
import app.domain.exception.InvalidTokenException
import app.domain.exception.InsufficientPermissionsException
import org.slf4j.LoggerFactory

/**
 * Middleware de autenticação JWT.
 * Valida o token e adiciona claims ao contexto da requisição.
 */
class AuthMiddleware : Handler {

    private val logger = LoggerFactory.getLogger(AuthMiddleware::class.java)

    override fun handle(ctx: Context) {
        val authHeader = ctx.header("Authorization")

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            logger.warn("Token não fornecido. Path: ${ctx.path()}, IP: ${ctx.ip()}")
            throw InvalidTokenException("Token de autenticação não fornecido")
        }

        val token = authHeader.substring(7).trim()

        val claims = JwtProvider.validateToken(token)

        if (claims == null) {
            logger.warn("Token inválido ou expirado. Path: ${ctx.path()}, IP: ${ctx.ip()}")
            throw InvalidTokenException("Token inválido ou expirado")
        }

        // Adicionar claims ao contexto para uso nos controllers
        ctx.attribute("userId", claims.userId)
        ctx.attribute("userEmail", claims.email)
        ctx.attribute("userRoles", claims.roles)
        ctx.attribute("jwtClaims", claims)
    }
}

/**
 * Middleware para verificar se o usuário possui uma role específica.
 */
class RequireRole(private val requiredRole: Role) : Handler {

    private val logger = LoggerFactory.getLogger(RequireRole::class.java)

    override fun handle(ctx: Context) {
        val roles = ctx.attribute<Set<Role>>("userRoles") ?: emptySet()
        val userId = ctx.attribute<Long>("userId")

        if (!roles.contains(requiredRole)) {
            logger.warn("Acesso negado. User: $userId, Required role: $requiredRole, Has: $roles")
            throw InsufficientPermissionsException(requiredRole.name)
        }
    }
}

/**
 * Extensions para facilitar acesso aos claims no contexto
 */
fun Context.userId(): Long = this.attribute<Long>("userId")!!
fun Context.userEmail(): String = this.attribute<String>("userEmail")!!
fun Context.userRoles(): Set<Role> = this.attribute<Set<Role>>("userRoles")!!
fun Context.jwtClaims(): JwtClaims = this.attribute<JwtClaims>("jwtClaims")!!
