package app.domain.repository

import app.domain.entity.FaceValidationToken

/**
 * Contrato do repositório de tokens de validação facial (DDD - Interface segregation)
 */
interface FaceValidationTokenRepository {
    fun findByToken(token: String): FaceValidationToken?
    fun findValidTokenByUserId(userId: Long): FaceValidationToken?
    fun save(token: FaceValidationToken): FaceValidationToken
    fun update(token: FaceValidationToken): FaceValidationToken
    fun deleteExpired()
    fun deleteByUserId(userId: Long)
}
