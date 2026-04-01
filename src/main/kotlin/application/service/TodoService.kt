package app.application.service

import app.domain.entity.Todo
import app.domain.repository.TodoRepository
import app.domain.exception.TodoNotFoundException
import app.domain.exception.TodoAccessDeniedException
import org.slf4j.LoggerFactory

/**
 * Serviço de gerenciamento de Todos (Application Layer - DDD)
 * Contém a lógica de negócio relacionada a tarefas.
 */
class TodoService(
    private val todoRepository: TodoRepository
) {

    private val logger = LoggerFactory.getLogger(TodoService::class.java)

    /**
     * Lista todos os todos de um usuário
     */
    fun listAll(userId: Long): List<Todo> {
        return todoRepository.findAll(userId)
    }

    /**
     * Busca um todo específico
     */
    fun getById(id: Long, userId: Long): Todo {
        val todo = todoRepository.findById(id, userId)
            ?: throw TodoNotFoundException("Todo não encontrado")

        // Verificar permissão
        if (!todo.belongsTo(userId)) {
            throw TodoAccessDeniedException("Você não tem permissão para acessar este todo")
        }

        return todo
    }

    /**
     * Cria um novo todo
     */
    fun create(title: String, userId: Long): Todo {
        val todo = Todo(
            id = 0, // Será gerado pelo banco
            userId = userId,
            title = title.trim()
        )

        val savedTodo = todoRepository.save(todo)

        logger.info("Todo criado: id={}, userId={}", savedTodo.id, userId)

        return savedTodo
    }

    /**
     * Alterna o status de um todo (done/undone)
     */
    fun toggle(id: Long, userId: Long): Todo {
        val todo = getById(id, userId)
        val toggledTodo = todo.toggle()

        val updatedTodo = todoRepository.update(toggledTodo)

        logger.info("Todo toggled: id={}, done={}", updatedTodo.id, updatedTodo.done)

        return updatedTodo
    }

    /**
     * Atualiza o título de um todo
     */
    fun updateTitle(id: Long, newTitle: String, userId: Long): Todo {
        val todo = getById(id, userId)
        val updatedTodo = todo.updateTitle(newTitle.trim())

        val saved = todoRepository.update(updatedTodo)

        logger.info("Todo title updated: id={}", saved.id)

        return saved
    }

    /**
     * Deleta um todo
     */
    fun delete(id: Long, userId: Long) {
        // Verificar se existe e se pertence ao usuário
        getById(id, userId)

        todoRepository.delete(id, userId)

        logger.info("Todo deleted: id={}, userId={}", id, userId)
    }
}
