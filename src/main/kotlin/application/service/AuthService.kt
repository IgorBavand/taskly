package app.application.service

import app.domain.entity.Email
import app.domain.entity.Role
import app.domain.entity.User
import app.domain.entity.RefreshToken
import app.domain.repository.UserRepository
import app.domain.repository.RefreshTokenRepository
import app.domain.exception.*
import app.security.*
import app.config.EnvConfig
import org.slf4j.LoggerFactory
import java.time.LocalDateTime

/**
 * Serviço de autenticação (Application Layer - DDD)
 * Contém a lógica de negócio relacionada a autenticação e autorização.
 */
class AuthService(
    private val userRepository: UserRepository,
    private val refreshTokenRepository: RefreshTokenRepository
) {

    private val logger = LoggerFactory.getLogger(AuthService::class.java)

    /**
     * Registra um novo usuário no sistema
     */
    fun register(email: String, password: String, ip: String): User {
        val emailVO = Email.of(email)

        // Verificar se email já existe
        if (userRepository.existsByEmail(emailVO)) {
            throw EmailAlreadyExistsException(email)
        }

        // Hash da senha
        val passwordHash = PasswordHasher.hash(password)

        // Criar usuário
        val user = User(
            id = 0, // Será gerado pelo banco
            email = emailVO,
            passwordHash = passwordHash,
            roles = setOf(Role.USER)
        )

        val savedUser = userRepository.save(user)

        AuditLogger.logRegistration(email, ip)
        logger.info("Novo usuário registrado: {}", email)

        return savedUser
    }

    /**
     * Autentica um usuário e retorna tokens
     */
    fun login(email: String, password: String, ip: String): AuthTokens {
        val emailVO = Email.of(email)

        val user = userRepository.findByEmail(emailVO)

        if (user == null) {
            AuditLogger.logAuthFailure(email, ip, "user_not_found")
            throw InvalidCredentialsException("Credenciais inválidas")
        }

        if (!user.active) {
            AuditLogger.logAuthFailure(email, ip, "account_inactive")
            throw AccountDisabledException()
        }

        if (!PasswordHasher.verify(password, user.passwordHash)) {
            AuditLogger.logAuthFailure(email, ip, "invalid_password")
            throw InvalidCredentialsException("Credenciais inválidas")
        }

        // Gerar tokens
        val accessToken = JwtProvider.generateToken(user.id, user.email.value, user.roles)
        val refreshToken = createRefreshToken(user.id)

        AuditLogger.logAuthSuccess(email, ip)
        logger.info("Login bem-sucedido: {}", email)

        return AuthTokens(
            accessToken = accessToken,
            refreshToken = refreshToken.token,
            expiresIn = EnvConfig.JWT_EXPIRATION_MS
        )
    }

    /**
     * Renova o access token usando um refresh token válido
     */
    fun refreshToken(refreshTokenString: String, ip: String): AuthTokens {
        val refreshToken = refreshTokenRepository.findByToken(refreshTokenString)
            ?: throw InvalidRefreshTokenException("Refresh token inválido")

        if (!refreshToken.isValid()) {
            throw InvalidRefreshTokenException("Refresh token expirado ou revogado")
        }

        val user = userRepository.findById(refreshToken.userId)
            ?: throw UserNotFoundException()

        if (!user.active) {
            throw AccountDisabledException()
        }

        // Gerar novo access token
        val accessToken = JwtProvider.generateToken(user.id, user.email.value, user.roles)

        // Opcionalmente, rotacionar refresh token (mais seguro)
        val newRefreshToken = rotateRefreshToken(refreshToken)

        AuditLogger.logTokenRefresh(user.id, ip)

        return AuthTokens(
            accessToken = accessToken,
            refreshToken = newRefreshToken.token,
            expiresIn = EnvConfig.JWT_EXPIRATION_MS
        )
    }

    /**
     * Revoga todos os refresh tokens de um usuário (logout global)
     */
    fun logout(userId: Long) {
        refreshTokenRepository.deleteByUserId(userId)
        logger.info("Logout realizado para usuário: {}", userId)
    }

    /**
     * Cria um novo refresh token
     */
    private fun createRefreshToken(userId: Long): RefreshToken {
        val token = RefreshTokenProvider.generate()
        val expiresAt = LocalDateTime.now().plusDays(EnvConfig.REFRESH_TOKEN_EXPIRATION_DAYS)

        val refreshToken = RefreshToken(
            id = 0,
            userId = userId,
            token = token,
            expiresAt = expiresAt
        )

        return refreshTokenRepository.save(refreshToken)
    }

    /**
     * Rotaciona refresh token (revoga o antigo e cria um novo)
     */
    private fun rotateRefreshToken(oldToken: RefreshToken): RefreshToken {
        // Revogar token antigo
        refreshTokenRepository.update(oldToken.revoke())

        // Criar novo token
        return createRefreshToken(oldToken.userId)
    }
}

/**
 * DTOs
 */
data class AuthTokens(
    val accessToken: String,
    val refreshToken: String,
    val expiresIn: Long
)
