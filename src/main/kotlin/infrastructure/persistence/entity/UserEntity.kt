package app.infrastructure.persistence.entity

import app.domain.entity.Email
import app.domain.entity.Role
import app.domain.entity.User
import jakarta.persistence.*

/**
 * Entidade JPA para User.
 *
 * Mapeia a tabela 'users' do banco de dados.
 * Separada da entidade de domínio para manter DDD limpo.
 */
@Entity
@Table(name = "users")
class UserEntity(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0,

    @Column(name = "email", nullable = false, unique = true, length = 255)
    var email: String = "",

    @Column(name = "password_hash", nullable = false, length = 255)
    var passwordHash: String = "",

    @Column(name = "roles", nullable = false, length = 255)
    var roles: String = "USER",

    @Column(name = "active", nullable = false)
    var active: Boolean = true

) : BaseEntity() {

    /**
     * Converte para entidade de domínio
     */
    fun toDomain(): User {
        return User(
            id = this.id,
            email = Email.of(this.email),
            passwordHash = this.passwordHash,
            roles = parseRoles(this.roles),
            active = this.active,
            createdAt = this.createdAt,
            updatedAt = this.updatedAt
        )
    }

    companion object {
        /**
         * Cria a partir da entidade de domínio
         */
        fun fromDomain(user: User): UserEntity {
            return UserEntity(
                id = user.id,
                email = user.email.value,
                passwordHash = user.passwordHash,
                roles = formatRoles(user.roles),
                active = user.active
            ).also {
                it.createdAt = user.createdAt
                it.updatedAt = user.updatedAt
            }
        }

        private fun formatRoles(roles: Set<Role>): String {
            return roles.joinToString(",") { it.name }
        }

        private fun parseRoles(rolesString: String): Set<Role> {
            return rolesString.split(",")
                .map { Role.valueOf(it.trim()) }
                .toSet()
        }
    }
}
