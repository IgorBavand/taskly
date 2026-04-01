package app.infrastructure.persistence.repository

import app.domain.entity.Email
import app.domain.entity.User
import app.domain.repository.UserRepository
import app.infrastructure.persistence.TransactionManager
import app.infrastructure.persistence.entity.UserEntity

/**
 * Implementação JPA do UserRepository.
 *
 * Utiliza EntityManager via TransactionManager para operações de persistência.
 * Converte entre UserEntity (JPA) e User (domínio).
 */
class JpaUserRepository(
    private val transactionManager: TransactionManager
) : UserRepository {

    override fun findById(id: Long): User? {
        return transactionManager.executeReadOnly { em ->
            val entity = em.find(UserEntity::class.java, id)
            entity?.toDomain()
        }
    }

    override fun findByEmail(email: Email): User? {
        return transactionManager.executeReadOnly { em ->
            val query = em.createQuery(
                "SELECT u FROM UserEntity u WHERE u.email = :email",
                UserEntity::class.java
            )
            query.setParameter("email", email.value)
            query.resultList.firstOrNull()?.toDomain()
        }
    }

    override fun existsByEmail(email: Email): Boolean {
        return transactionManager.executeReadOnly { em ->
            val query = em.createQuery(
                "SELECT COUNT(u) FROM UserEntity u WHERE u.email = :email",
                Long::class.java
            )
            query.setParameter("email", email.value)
            query.singleResult > 0
        }
    }

    override fun save(user: User): User {
        return transactionManager.executeInTransaction { em ->
            val entity = UserEntity.fromDomain(user)
            em.persist(entity)
            em.flush() // Força a geração do ID
            entity.toDomain()
        }
    }

    override fun update(user: User): User {
        return transactionManager.executeInTransaction { em ->
            val entity = UserEntity.fromDomain(user)
            val merged = em.merge(entity)
            em.flush()
            merged.toDomain()
        }
    }
}
