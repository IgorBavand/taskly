package app.infrastructure.persistence.entity

import jakarta.persistence.*
import java.time.LocalDateTime

/**
 * Classe base para todas as entidades JPA.
 *
 * Fornece campos comuns de auditoria:
 * - createdAt: data/hora de criação
 * - updatedAt: data/hora da última atualização
 *
 * Utiliza @MappedSuperclass para que os campos sejam herdados pelas entidades filhas
 * sem criar uma tabela separada.
 */
@MappedSuperclass
abstract class BaseEntity {

    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: LocalDateTime = LocalDateTime.now()
        protected set

    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now()
        protected set

    /**
     * Callback executado antes de persist (INSERT)
     */
    @PrePersist
    protected fun prePersist() {
        val now = LocalDateTime.now()
        createdAt = now
        updatedAt = now
    }

    /**
     * Callback executado antes de update
     */
    @PreUpdate
    protected fun preUpdate() {
        updatedAt = LocalDateTime.now()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is BaseEntity) return false

        if (createdAt != other.createdAt) return false
        if (updatedAt != other.updatedAt) return false

        return true
    }

    override fun hashCode(): Int {
        var result = createdAt.hashCode()
        result = 31 * result + updatedAt.hashCode()
        return result
    }
}
