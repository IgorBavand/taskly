package app.security

import org.mindrot.jbcrypt.BCrypt
import app.config.EnvConfig
import app.domain.exception.InvalidPasswordException

/**
 * Serviço de hashing de senhas usando BCrypt.
 * BCrypt é resistente a ataques de força bruta por ser computacionalmente caro.
 */
object PasswordHasher {

    fun hash(plainPassword: String): String {
        validatePassword(plainPassword)
        return BCrypt.hashpw(plainPassword, BCrypt.gensalt(EnvConfig.BCRYPT_ROUNDS))
    }

    fun verify(plainPassword: String, hashedPassword: String): Boolean {
        return try {
            BCrypt.checkpw(plainPassword, hashedPassword)
        } catch (e: Exception) {
            false
        }
    }

    private fun validatePassword(password: String) {
        when {
            password.length < 8 -> throw InvalidPasswordException("Senha deve ter no mínimo 8 caracteres")
            password.length > 128 -> throw InvalidPasswordException("Senha muito longa (máximo 128 caracteres)")
            !password.any { it.isUpperCase() } -> throw InvalidPasswordException("Senha deve conter pelo menos uma letra maiúscula")
            !password.any { it.isLowerCase() } -> throw InvalidPasswordException("Senha deve conter pelo menos uma letra minúscula")
            !password.any { it.isDigit() } -> throw InvalidPasswordException("Senha deve conter pelo menos um número")
        }
    }
}
