package app.domain.repository

import app.domain.entity.RefreshToken

/**
 * Contrato do repositório de refresh tokens (DDD - Interface segregation)
 */
interface RefreshTokenRepository {
    fun findByToken(token: String): RefreshToken?
    fun findAllByUserId(userId: Long): List<RefreshToken>
    fun save(refreshToken: RefreshToken): RefreshToken
    fun update(refreshToken: RefreshToken): RefreshToken
    fun deleteByUserId(userId: Long)
    fun deleteExpired()
}
