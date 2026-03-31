package app

import app.config.DatabaseConfig
import app.config.JavalinConfig
import app.controller.TodoController
import app.repository.TodoRepository
import app.service.TodoService


fun main() {

    val app = JavalinConfig.create()
    val dataSource = DatabaseConfig.dataSource()

    // DI manual (simples e eficiente)
    val repository = TodoRepository(dataSource)
    val service = TodoService(repository)
    val controller = TodoController(service)

    controller.register(app)

    app.start(7000)
}