package app.config

/**
 * Centraliza todas as configurações da aplicação vindas de variáveis de ambiente.
 * Segue o princípio de fail-fast: se configurações críticas não existirem, a aplicação não inicia.
 */
object EnvConfig {

    // Database
    val DATABASE_URL: String = getEnv("DATABASE_URL", "jdbc:postgresql://localhost:5432/taskly")
    val DATABASE_USER: String = getEnv("DATABASE_USER", "postgres")
    val DATABASE_PASSWORD: String = getEnvRequired("DATABASE_PASSWORD", "postgres") // Em prod, deve ser obrigatória

    // JWT
    val JWT_SECRET: String = getEnvRequired("JWT_SECRET", generateDefaultSecret())
    val JWT_ISSUER: String = getEnv("JWT_ISSUER", "taskly-api")
    val JWT_EXPIRATION_MS: Long = getEnv("JWT_EXPIRATION_MS", "3600000").toLong() // 1 hora
    val REFRESH_TOKEN_EXPIRATION_DAYS: Long = getEnv("REFRESH_TOKEN_EXPIRATION_DAYS", "30").toLong()

    // Server
    val SERVER_PORT: Int = getEnv("SERVER_PORT", "7171").toInt()
    val ENVIRONMENT: String = getEnv("ENVIRONMENT", "development") // development, production

    // Security
    val BCRYPT_ROUNDS: Int = getEnv("BCRYPT_ROUNDS", "12").toInt()
    val RATE_LIMIT_MAX_REQUESTS: Int = getEnv("RATE_LIMIT_MAX_REQUESTS", "100").toInt()
    val RATE_LIMIT_WINDOW_SECONDS: Long = getEnv("RATE_LIMIT_WINDOW_SECONDS", "60").toLong()
    val AUTH_RATE_LIMIT_MAX_REQUESTS: Int = getEnv("AUTH_RATE_LIMIT_MAX_REQUESTS", "5").toInt()

    // CORS
    val ALLOWED_ORIGINS: List<String> = getEnv("ALLOWED_ORIGINS", "http://localhost:3000")
        .split(",")
        .map { it.trim() }

    private fun getEnv(key: String, default: String): String {
        return System.getenv(key) ?: default
    }

    private fun getEnvRequired(key: String, devDefault: String? = null): String {
        val value = System.getenv(key)

        if (value.isNullOrBlank()) {
            if (ENVIRONMENT == "production") {
                throw IllegalStateException("Variável de ambiente obrigatória não configurada: $key")
            }
            return devDefault ?: throw IllegalStateException("$key não configurada e sem valor padrão")
        }

        return value
    }

    private fun generateDefaultSecret(): String {
        // Em desenvolvimento, gera um secret aleatório (não use em produção!)
        if (ENVIRONMENT != "production") {
            println("⚠️  AVISO: Usando JWT_SECRET gerado automaticamente. Configure JWT_SECRET em produção!")
            return java.util.UUID.randomUUID().toString() + java.util.UUID.randomUUID().toString()
        }
        throw IllegalStateException("JWT_SECRET deve ser configurado em produção")
    }

    fun isProduction(): Boolean = ENVIRONMENT == "production"
    fun isDevelopment(): Boolean = ENVIRONMENT == "development"
}
