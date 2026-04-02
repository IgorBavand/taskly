package app.infrastructure.persistence.repository

import app.domain.entity.FaceValidationToken
import app.domain.repository.FaceValidationTokenRepository
import app.infrastructure.persistence.TransactionManager
import app.infrastructure.persistence.entity.FaceValidationTokenEntity
import java.time.LocalDateTime

/**
 * Implementação JPA do FaceValidationTokenRepository.
 *
 * Utiliza EntityManager via TransactionManager para operações de persistência.
 * Converte entre FaceValidationTokenEntity (JPA) e FaceValidationToken (domínio).
 */
class JpaFaceValidationTokenRepository(
    private val transactionManager: TransactionManager
) : FaceValidationTokenRepository {

    override fun findByToken(token: String): FaceValidationToken? {
        return transactionManager.executeReadOnly { em ->
            val query = em.createQuery(
                "SELECT t FROM FaceValidationTokenEntity t WHERE t.token = :token",
                FaceValidationTokenEntity::class.java
            )
            query.setParameter("token", token)
            query.resultList.firstOrNull()?.toDomain()
        }
    }

    override fun findValidTokenByUserId(userId: Long): FaceValidationToken? {
        return transactionManager.executeReadOnly { em ->
            val query = em.createQuery(
                """
                SELECT t FROM FaceValidationTokenEntity t
                WHERE t.userId = :userId
                AND t.used = false
                AND t.expiresAt > :now
                ORDER BY t.createdAt DESC
                """.trimIndent(),
                FaceValidationTokenEntity::class.java
            )
            query.setParameter("userId", userId)
            query.setParameter("now", LocalDateTime.now())
            query.setMaxResults(1)
            query.resultList.firstOrNull()?.toDomain()
        }
    }

    override fun save(token: FaceValidationToken): FaceValidationToken {
        return transactionManager.executeInTransaction { em ->
            val entity = FaceValidationTokenEntity.fromDomain(token)
            em.persist(entity)
            em.flush()
            entity.toDomain()
        }
    }

    override fun update(token: FaceValidationToken): FaceValidationToken {
        return transactionManager.executeInTransaction { em ->
            val entity = FaceValidationTokenEntity.fromDomain(token)
            val merged = em.merge(entity)
            em.flush()
            merged.toDomain()
        }
    }

    override fun deleteExpired() {
        transactionManager.executeInTransaction { em ->
            val query = em.createQuery(
                "DELETE FROM FaceValidationTokenEntity t WHERE t.expiresAt < :now"
            )
            query.setParameter("now", LocalDateTime.now())
            query.executeUpdate()
        }
    }

    override fun deleteByUserId(userId: Long) {
        transactionManager.executeInTransaction { em ->
            val query = em.createQuery(
                "DELETE FROM FaceValidationTokenEntity t WHERE t.userId = :userId"
            )
            query.setParameter("userId", userId)
            query.executeUpdate()
        }
    }
}
