package app.security

import java.security.SecureRandom
import java.util.*

/**
 * Provider de Refresh Tokens.
 * Gera tokens criptograficamente seguros para renovação de JWT.
 */
object RefreshTokenProvider {

    private val random = SecureRandom()

    fun generate(): String {
        val bytes = ByteArray(32) // 256 bits
        random.nextBytes(bytes)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }
}
