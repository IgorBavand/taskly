package app.infrastructure.persistence.repository

import app.domain.entity.RefreshToken
import app.domain.repository.RefreshTokenRepository
import app.infrastructure.persistence.TransactionManager
import app.infrastructure.persistence.entity.RefreshTokenEntity
import java.time.LocalDateTime

/**
 * Implementação JPA do RefreshTokenRepository.
 *
 * Utiliza EntityManager via TransactionManager para operações de persistência.
 * Converte entre RefreshTokenEntity (JPA) e RefreshToken (domínio).
 */
class JpaRefreshTokenRepository(
    private val transactionManager: TransactionManager
) : RefreshTokenRepository {

    override fun findByToken(token: String): RefreshToken? {
        return transactionManager.executeReadOnly { em ->
            val query = em.createQuery(
                "SELECT r FROM RefreshTokenEntity r WHERE r.token = :token",
                RefreshTokenEntity::class.java
            )
            query.setParameter("token", token)
            query.resultList.firstOrNull()?.toDomain()
        }
    }

    override fun findAllByUserId(userId: Long): List<RefreshToken> {
        return transactionManager.executeReadOnly { em ->
            val query = em.createQuery(
                "SELECT r FROM RefreshTokenEntity r WHERE r.userId = :userId",
                RefreshTokenEntity::class.java
            )
            query.setParameter("userId", userId)
            query.resultList.map { it.toDomain() }
        }
    }

    override fun save(refreshToken: RefreshToken): RefreshToken {
        return transactionManager.executeInTransaction { em ->
            val entity = RefreshTokenEntity.fromDomain(refreshToken)
            em.persist(entity)
            em.flush() // Força a geração do ID
            entity.toDomain()
        }
    }

    override fun update(refreshToken: RefreshToken): RefreshToken {
        return transactionManager.executeInTransaction { em ->
            val entity = RefreshTokenEntity.fromDomain(refreshToken)
            val merged = em.merge(entity)
            em.flush()
            merged.toDomain()
        }
    }

    override fun deleteByUserId(userId: Long) {
        transactionManager.executeInTransaction { em ->
            val query = em.createQuery(
                "DELETE FROM RefreshTokenEntity r WHERE r.userId = :userId"
            )
            query.setParameter("userId", userId)
            query.executeUpdate()
        }
    }

    override fun deleteExpired() {
        transactionManager.executeInTransaction { em ->
            val query = em.createQuery(
                "DELETE FROM RefreshTokenEntity r WHERE r.expiresAt < :now"
            )
            query.setParameter("now", LocalDateTime.now())
            query.executeUpdate()
        }
    }
}
