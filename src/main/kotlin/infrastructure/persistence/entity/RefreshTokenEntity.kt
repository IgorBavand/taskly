package app.infrastructure.persistence.entity

import app.domain.entity.RefreshToken
import jakarta.persistence.*
import java.time.LocalDateTime

/**
 * Entidade JPA para RefreshToken.
 *
 * Mapeia a tabela 'refresh_tokens' do banco de dados.
 * Separada da entidade de domínio para manter DDD limpo.
 */
@Entity
@Table(name = "refresh_tokens")
class RefreshTokenEntity(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0,

    @Column(name = "user_id", nullable = false)
    var userId: Long = 0,

    @Column(name = "token", nullable = false, unique = true, length = 512)
    var token: String = "",

    @Column(name = "expires_at", nullable = false)
    var expiresAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "revoked", nullable = false)
    var revoked: Boolean = false

) : BaseEntity() {

    /**
     * Converte para entidade de domínio
     */
    fun toDomain(): RefreshToken {
        return RefreshToken(
            id = this.id,
            userId = this.userId,
            token = this.token,
            expiresAt = this.expiresAt,
            createdAt = this.createdAt,
            revoked = this.revoked
        )
    }

    companion object {
        /**
         * Cria a partir da entidade de domínio
         */
        fun fromDomain(refreshToken: RefreshToken): RefreshTokenEntity {
            return RefreshTokenEntity(
                id = refreshToken.id,
                userId = refreshToken.userId,
                token = refreshToken.token,
                expiresAt = refreshToken.expiresAt,
                revoked = refreshToken.revoked
            ).also {
                it.createdAt = refreshToken.createdAt
                // updatedAt será gerenciado automaticamente pelo BaseEntity
            }
        }
    }
}
