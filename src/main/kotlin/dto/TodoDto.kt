package app.dto

import java.time.LocalDateTime

/**
 * Request para criação
 */
data class CreateTodoRequest(
    val title: String
)

/**
 * Request para update parcial (boa prática: PATCH)
 */
data class UpdateTodoRequest(
    val title: String? = null,
    val done: Boolean? = null
)

/**
 * Response padrão
 */
data class TodoResponse(
    val id: Long,
    val title: String,
    val done: Boolean,
    val createdAt: LocalDateTime? = null
)