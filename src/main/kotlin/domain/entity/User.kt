package app.domain.entity

import java.time.LocalDateTime

/**
 * Entidade de domínio: User
 * Representa um usuário do sistema com suas credenciais e permissões.
 */
data class User(
    val id: Long,
    val email: Email,
    val passwordHash: String,
    val roles: Set<Role> = setOf(Role.USER),
    val active: Boolean = true,
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val updatedAt: LocalDateTime = LocalDateTime.now()
) {
    fun hasRole(role: Role): Boolean = roles.contains(role)

    fun isAdmin(): Boolean = hasRole(Role.ADMIN)

    fun withUpdatedPassword(newPasswordHash: String): User {
        return copy(passwordHash = newPasswordHash, updatedAt = LocalDateTime.now())
    }

    fun activate(): User = copy(active = true, updatedAt = LocalDateTime.now())

    fun deactivate(): User = copy(active = false, updatedAt = LocalDateTime.now())
}

/**
 * Enum de roles do sistema (RBAC)
 */
enum class Role {
    USER,
    ADMIN
}
