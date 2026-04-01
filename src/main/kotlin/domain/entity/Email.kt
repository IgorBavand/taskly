package app.domain.entity

/**
 * Value Object: Email
 * Garante que emails sejam sempre válidos.
 */
data class Email(val value: String) {

    init {
        require(value.isNotBlank()) { "Email não pode ser vazio" }
        require(isValid(value)) { "Email inválido: $value" }
    }

    companion object {
        private val EMAIL_REGEX = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$".toRegex()

        fun isValid(email: String): Boolean {
            return email.matches(EMAIL_REGEX)
        }

        fun of(value: String): Email = Email(value.trim().lowercase())
    }

    override fun toString(): String = value
}
