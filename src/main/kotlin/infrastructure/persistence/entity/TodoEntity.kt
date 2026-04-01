package app.infrastructure.persistence.entity

import app.domain.entity.Todo
import jakarta.persistence.*

/**
 * Entidade JPA para Todo.
 *
 * Mapeia a tabela 'todos' do banco de dados.
 * Separada da entidade de domínio para manter DDD limpo.
 */
@Entity
@Table(name = "todos")
class TodoEntity(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0,

    @Column(name = "user_id", nullable = false)
    var userId: Long = 0,

    @Column(name = "title", nullable = false, length = 255)
    var title: String = "",

    @Column(name = "done", nullable = false)
    var done: Boolean = false

) : BaseEntity() {

    /**
     * Converte para entidade de domínio
     */
    fun toDomain(): Todo {
        return Todo(
            id = this.id,
            userId = this.userId,
            title = this.title,
            done = this.done,
            createdAt = this.createdAt,
            updatedAt = this.updatedAt
        )
    }

    companion object {
        /**
         * Cria a partir da entidade de domínio
         */
        fun fromDomain(todo: Todo): TodoEntity {
            return TodoEntity(
                id = todo.id,
                userId = todo.userId,
                title = todo.title,
                done = todo.done
            ).also {
                it.createdAt = todo.createdAt
                it.updatedAt = todo.updatedAt
            }
        }
    }
}
