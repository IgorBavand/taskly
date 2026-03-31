package app.service

import app.model.Todo
import app.repository.TodoRepository


class TodoService(private val repository: TodoRepository) {

    fun list() = repository.findAll()

    fun get(id: Long) =
        repository.findById(id)
            ?: throw IllegalArgumentException("Todo not found")

    fun create(title: String): Todo {
        require(title.isNotBlank()) { "Title is required" }
        return repository.create(title)
    }

    fun toggle(id: Long) = repository.toggle(id)

    fun delete(id: Long) = repository.delete(id)
}