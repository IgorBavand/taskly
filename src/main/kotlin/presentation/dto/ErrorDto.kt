package app.presentation.dto

import java.time.LocalDateTime

/**
 * DTOs para respostas de erro
 */

data class ErrorResponse(
    val error: String,
    val message: String? = null,
    val timestamp: LocalDateTime = LocalDateTime.now(),
    val path: String? = null
)

data class ValidationErrorResponse(
    val error: String,
    val violations: List<ValidationViolation>,
    val timestamp: LocalDateTime = LocalDateTime.now()
)

data class ValidationViolation(
    val field: String,
    val message: String
)
