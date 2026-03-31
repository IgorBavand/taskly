package app.mapper

import app.model.Todo
import app.dto.TodoResponse

object TodoMapper {

    fun toResponse(todo: Todo): TodoResponse {
        return TodoResponse(
            id = todo.id,
            title = todo.title,
            done = todo.done,
            createdAt = todo.createdAt
        )
    }
}