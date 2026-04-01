package app.domain.entity

import java.time.LocalDateTime

/**
 * Entidade de domínio: RefreshToken
 * Representa um token de refresh para renovação de JWT.
 */
data class RefreshToken(
    val id: Long,
    val userId: Long,
    val token: String,
    val expiresAt: LocalDateTime,
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val revoked: Boolean = false
) {
    fun isExpired(): Boolean = LocalDateTime.now().isAfter(expiresAt)

    fun isValid(): Boolean = !revoked && !isExpired()

    fun revoke(): RefreshToken = copy(revoked = true)
}
