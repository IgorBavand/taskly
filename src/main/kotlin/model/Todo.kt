package app.model

import java.time.LocalDateTime

data class Todo(
    val id: Long,
    val title: String,
    val done: Boolean,
    val createdAt: LocalDateTime
)