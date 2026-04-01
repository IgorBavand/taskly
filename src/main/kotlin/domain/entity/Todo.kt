package app.domain.entity

import java.time.LocalDateTime

/**
 * Entidade de domínio: Todo
 * Representa uma tarefa do usuário.
 */
data class Todo(
    val id: Long,
    val userId: Long,
    val title: String,
    val done: Boolean = false,
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val updatedAt: LocalDateTime = LocalDateTime.now()
) {
    init {
        require(title.isNotBlank()) { "Título não pode ser vazio" }
        require(title.length <= 500) { "Título não pode ter mais de 500 caracteres" }
    }

    fun toggle(): Todo {
        return copy(done = !done, updatedAt = LocalDateTime.now())
    }

    fun updateTitle(newTitle: String): Todo {
        require(newTitle.isNotBlank()) { "Título não pode ser vazio" }
        require(newTitle.length <= 500) { "Título não pode ter mais de 500 caracteres" }
        return copy(title = newTitle, updatedAt = LocalDateTime.now())
    }

    fun belongsTo(userId: Long): Boolean = this.userId == userId
}
