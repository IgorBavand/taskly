package app.infrastructure.persistence.repository

import app.domain.entity.Todo
import app.domain.repository.TodoRepository
import app.infrastructure.persistence.TransactionManager
import app.infrastructure.persistence.entity.TodoEntity

/**
 * Implementação JPA do TodoRepository.
 *
 * Utiliza EntityManager via TransactionManager para operações de persistência.
 * Converte entre TodoEntity (JPA) e Todo (domínio).
 * Garante isolamento por usuário em todas as operações.
 */
class JpaTodoRepository(
    private val transactionManager: TransactionManager
) : TodoRepository {

    override fun findAll(userId: Long): List<Todo> {
        return transactionManager.executeReadOnly { em ->
            val query = em.createQuery(
                "SELECT t FROM TodoEntity t WHERE t.userId = :userId ORDER BY t.createdAt DESC",
                TodoEntity::class.java
            )
            query.setParameter("userId", userId)
            query.resultList.map { it.toDomain() }
        }
    }

    override fun findById(id: Long, userId: Long): Todo? {
        return transactionManager.executeReadOnly { em ->
            val query = em.createQuery(
                "SELECT t FROM TodoEntity t WHERE t.id = :id AND t.userId = :userId",
                TodoEntity::class.java
            )
            query.setParameter("id", id)
            query.setParameter("userId", userId)
            query.resultList.firstOrNull()?.toDomain()
        }
    }

    override fun save(todo: Todo): Todo {
        return transactionManager.executeInTransaction { em ->
            val entity = TodoEntity.fromDomain(todo)
            em.persist(entity)
            em.flush() // Força a geração do ID
            entity.toDomain()
        }
    }

    override fun update(todo: Todo): Todo {
        return transactionManager.executeInTransaction { em ->
            val entity = TodoEntity.fromDomain(todo)
            val merged = em.merge(entity)
            em.flush()
            merged.toDomain()
        }
    }

    override fun delete(id: Long, userId: Long) {
        transactionManager.executeInTransaction { em ->
            val query = em.createQuery(
                "DELETE FROM TodoEntity t WHERE t.id = :id AND t.userId = :userId"
            )
            query.setParameter("id", id)
            query.setParameter("userId", userId)
            query.executeUpdate()
        }
    }
}
