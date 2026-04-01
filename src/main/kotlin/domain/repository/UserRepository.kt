package app.domain.repository

import app.domain.entity.Email
import app.domain.entity.User

/**
 * Contrato do repositório de usuários (DDD - Interface segregation)
 */
interface UserRepository {
    fun findById(id: Long): User?
    fun findByEmail(email: Email): User?
    fun existsByEmail(email: Email): Boolean
    fun save(user: User): User
    fun update(user: User): User
}
