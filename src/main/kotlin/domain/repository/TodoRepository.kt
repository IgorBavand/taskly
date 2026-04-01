package app.domain.repository

import app.domain.entity.Todo

/**
 * Contrato do repositório de todos (DDD - Interface segregation)
 */
interface TodoRepository {
    fun findAll(userId: Long): List<Todo>
    fun findById(id: Long, userId: Long): Todo?
    fun save(todo: Todo): Todo
    fun update(todo: Todo): Todo
    fun delete(id: Long, userId: Long)
}
