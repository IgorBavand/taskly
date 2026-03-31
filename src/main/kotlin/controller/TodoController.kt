package app.controller

import app.service.TodoService
import app.dto.CreateTodoRequest
import app.dto.TodoResponse
import io.javalin.Javalin
import java.time.LocalDateTime

class TodoController(private val service: TodoService) {

    fun register(app: Javalin) {

        app.get("/todos") { ctx ->
            val response = service.list().map {
                TodoResponse(it.id, it.title, it.done)
            }
            ctx.json(response)
        }

        app.get("/todos/{id}") { ctx ->
            val id = ctx.pathParam("id").toLong()
            val todo = service.get(id)
            ctx.json(TodoResponse(todo.id, todo.title, todo.done))
        }

        app.post("/todos") { ctx ->
            val request = ctx.bodyAsClass(CreateTodoRequest::class.java)
            val todo = service.create(request.title)
//            ctx.status(201)
            ctx.status(201)
        }

        app.patch("/todos/{id}/toggle") { ctx ->
            val id = ctx.pathParam("id").toLong()
            val todo = service.toggle(id)
            ctx.json(TodoResponse(todo.id, todo.title, todo.done))
        }

        app.delete("/todos/{id}") { ctx ->
            val id = ctx.pathParam("id").toLong()
            service.delete(id)
            ctx.status(204)
        }
    }
}