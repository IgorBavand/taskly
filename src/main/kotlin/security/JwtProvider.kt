package app.security

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.exceptions.JWTVerificationException
import app.config.EnvConfig
import app.domain.entity.Role
import java.util.*

/**
 * Provider de JWT para geração e validação de tokens.
 * Utiliza HMAC256 para assinatura.
 */
object JwtProvider {

    private val algorithm = Algorithm.HMAC256(EnvConfig.JWT_SECRET)

    private val verifier = JWT.require(algorithm)
        .withIssuer(EnvConfig.JWT_ISSUER)
        .build()

    fun generateToken(userId: Long, email: String, roles: Set<Role>): String {
        val now = Date()
        val expiresAt = Date(now.time + EnvConfig.JWT_EXPIRATION_MS)

        return JWT.create()
            .withIssuer(EnvConfig.JWT_ISSUER)
            .withSubject(userId.toString())
            .withClaim("email", email)
            .withClaim("roles", roles.map { it.name })
            .withIssuedAt(now)
            .withExpiresAt(expiresAt)
            .sign(algorithm)
    }

    fun validateToken(token: String): JwtClaims? {
        return try {
            val decoded = verifier.verify(token)

            val rolesString = decoded.getClaim("roles").asList(String::class.java)
            val roles = rolesString.map { Role.valueOf(it) }.toSet()

            JwtClaims(
                userId = decoded.subject.toLong(),
                email = decoded.getClaim("email").asString(),
                roles = roles
            )
        } catch (e: JWTVerificationException) {
            null
        } catch (e: Exception) {
            null
        }
    }
}

/**
 * Claims extraídos do JWT
 */
data class JwtClaims(
    val userId: Long,
    val email: String,
    val roles: Set<Role>
) {
    fun hasRole(role: Role): Boolean = roles.contains(role)
    fun isAdmin(): Boolean = hasRole(Role.ADMIN)
}
