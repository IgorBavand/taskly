package app.presentation.dto

import app.domain.entity.Todo
import java.time.LocalDateTime

/**
 * DTOs para operações de todos
 */

data class CreateTodoRequest(
    val title: String
)

data class UpdateTodoRequest(
    val title: String
)

data class TodoResponse(
    val id: Long,
    val title: String,
    val done: Boolean,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
) {
    companion object {
        fun fromEntity(todo: Todo): TodoResponse {
            return TodoResponse(
                id = todo.id,
                title = todo.title,
                done = todo.done,
                createdAt = todo.createdAt,
                updatedAt = todo.updatedAt
            )
        }
    }
}
