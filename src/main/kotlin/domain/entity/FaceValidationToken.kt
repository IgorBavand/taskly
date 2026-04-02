package app.domain.entity

import java.time.LocalDateTime
import java.util.UUID

/**
 * Entidade de domínio: FaceValidationToken
 *
 * Representa um token temporário emitido após validação facial bem-sucedida.
 * Permite que o usuário crie tarefas sem precisar validar o rosto novamente
 * durante um curto período de tempo.
 */
data class FaceValidationToken(
    val id: Long = 0,
    val userId: Long,
    val token: String = UUID.randomUUID().toString(),
    val expiresAt: LocalDateTime,
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val used: Boolean = false
) {
    companion object {
        // Token válido por 2 minutos
        private const val VALIDITY_MINUTES = 2L

        /**
         * Cria um novo token de validação facial para o usuário
         */
        fun create(userId: Long): FaceValidationToken {
            val now = LocalDateTime.now()
            return FaceValidationToken(
                userId = userId,
                expiresAt = now.plusMinutes(VALIDITY_MINUTES),
                createdAt = now
            )
        }
    }

    /**
     * Verifica se o token está expirado
     */
    fun isExpired(): Boolean = LocalDateTime.now().isAfter(expiresAt)

    /**
     * Verifica se o token é válido (não expirado e não usado)
     */
    fun isValid(): Boolean = !isExpired() && !used

    /**
     * Marca o token como usado (consumido)
     */
    fun markAsUsed(): FaceValidationToken = copy(used = true)
}
