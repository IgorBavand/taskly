package app.infrastructure.persistence.entity

import app.domain.entity.FaceValidationToken
import jakarta.persistence.*
import java.time.LocalDateTime

/**
 * Entidade JPA para FaceValidationToken.
 *
 * Mapeia a tabela 'face_validation_tokens' do banco de dados.
 * Separada da entidade de domínio para manter DDD limpo.
 */
@Entity
@Table(name = "face_validation_tokens")
class FaceValidationTokenEntity(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0,

    @Column(name = "user_id", nullable = false)
    var userId: Long = 0,

    @Column(name = "token", nullable = false, unique = true, length = 255)
    var token: String = "",

    @Column(name = "expires_at", nullable = false)
    var expiresAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "used", nullable = false)
    var used: Boolean = false

) : BaseEntity() {

    /**
     * Converte para entidade de domínio
     */
    fun toDomain(): FaceValidationToken {
        return FaceValidationToken(
            id = this.id,
            userId = this.userId,
            token = this.token,
            expiresAt = this.expiresAt,
            createdAt = this.createdAt,
            used = this.used
        )
    }

    companion object {
        /**
         * Cria a partir da entidade de domínio
         */
        fun fromDomain(token: FaceValidationToken): FaceValidationTokenEntity {
            return FaceValidationTokenEntity(
                id = token.id,
                userId = token.userId,
                token = token.token,
                expiresAt = token.expiresAt,
                used = token.used
            ).also {
                it.createdAt = token.createdAt
            }
        }
    }
}
