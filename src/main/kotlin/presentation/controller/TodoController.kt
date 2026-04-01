package app.presentation.controller

import app.application.service.TodoService
import app.presentation.dto.*
import app.security.AuthMiddleware
import app.security.userId
import io.javalin.Javalin
import io.javalin.http.Context

/**
 * Controller de Todos (Presentation Layer - DDD)
 *
 * Todas as exceptions são tratadas pelo GlobalExceptionHandler.
 * Código limpo e focado apenas na lógica de apresentação.
 */
class TodoController(private val todoService: TodoService) {

    fun register(app: Javalin) {
        // Todas as rotas de todos requerem autenticação
        app.before("/api/v1/todos*", AuthMiddleware())

        app.get("/api/v1/todos", ::handleListTodos)
        app.get("/api/v1/todos/{id}", ::handleGetTodo)
        app.post("/api/v1/todos", ::handleCreateTodo)
        app.patch("/api/v1/todos/{id}/toggle", ::handleToggleTodo)
        app.patch("/api/v1/todos/{id}", ::handleUpdateTodo)
        app.delete("/api/v1/todos/{id}", ::handleDeleteTodo)
    }

    private fun handleListTodos(ctx: Context) {
        val userId = ctx.userId()
        val todos = todoService.listAll(userId)
        val response = todos.map { TodoResponse.fromEntity(it) }

        ctx.json(response)
    }

    private fun handleGetTodo(ctx: Context) {
        val userId = ctx.userId()
        val id = ctx.pathParam("id").toLong()
        val todo = todoService.getById(id, userId)

        ctx.json(TodoResponse.fromEntity(todo))
    }

    private fun handleCreateTodo(ctx: Context) {
        val userId = ctx.userId()
        val request = ctx.bodyAsClass(CreateTodoRequest::class.java)
        val todo = todoService.create(request.title, userId)

        ctx.status(201).json(TodoResponse.fromEntity(todo))
    }

    private fun handleToggleTodo(ctx: Context) {
        val userId = ctx.userId()
        val id = ctx.pathParam("id").toLong()
        val todo = todoService.toggle(id, userId)

        ctx.json(TodoResponse.fromEntity(todo))
    }

    private fun handleUpdateTodo(ctx: Context) {
        val userId = ctx.userId()
        val id = ctx.pathParam("id").toLong()
        val request = ctx.bodyAsClass(UpdateTodoRequest::class.java)
        val todo = todoService.updateTitle(id, request.title, userId)

        ctx.json(TodoResponse.fromEntity(todo))
    }

    private fun handleDeleteTodo(ctx: Context) {
        val userId = ctx.userId()
        val id = ctx.pathParam("id").toLong()

        todoService.delete(id, userId)

        ctx.status(204)
    }
}
