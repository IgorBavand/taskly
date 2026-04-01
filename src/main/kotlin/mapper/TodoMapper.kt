package app.mapper

import app.domain.entity.Todo
import app.presentation.dto.TodoResponse

object TodoMapper {

    fun toResponse(todo: Todo): TodoResponse {
        return TodoResponse(
            id = todo.id,
            title = todo.title,
            done = todo.done,
            createdAt = todo.createdAt,
            updatedAt = todo.updatedAt
        )
    }
}