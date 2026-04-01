package app.config

import io.javalin.Javalin
import io.javalin.http.Context
import org.slf4j.LoggerFactory
import java.util.UUID

/**
 * Configuração do servidor Javalin com todas as configurações de segurança.
 */
object JavalinConfig {

    private val logger = LoggerFactory.getLogger(JavalinConfig::class.java)

    fun create(): Javalin {
        return Javalin.create { config ->
            // Logging apenas em desenvolvimento
            if (EnvConfig.isDevelopment()) {
                config.plugins.enableDevLogging()
            }

            // CORS
            config.plugins.enableCors { cors ->
                cors.add { corsConfig ->
                    EnvConfig.ALLOWED_ORIGINS.forEach { origin ->
                        corsConfig.allowHost(origin)
                    }
                    corsConfig.allowCredentials = true
                }
            }

            // JSON mapper (Jackson)
            config.jsonMapper(createJacksonMapper())

        }.before { ctx ->
            addSecurityHeaders(ctx)
            addRequestId(ctx)
            logRequest(ctx)

        }.after { ctx ->
            logResponse(ctx)
        }
    }

    private fun addSecurityHeaders(ctx: Context) {
        ctx.header("X-Content-Type-Options", "nosniff")
        ctx.header("X-Frame-Options", "DENY")
        ctx.header("X-XSS-Protection", "1; mode=block")
        ctx.header("Referrer-Policy", "strict-origin-when-cross-origin")
        ctx.header("Content-Security-Policy", "default-src 'self'")

        // HSTS apenas em produção
        if (EnvConfig.isProduction()) {
            ctx.header("Strict-Transport-Security", "max-age=31536000; includeSubDomains")
        }

        // Remove header que expõe tecnologia
        ctx.res().setHeader("Server", "")
    }

    private fun addRequestId(ctx: Context) {
        val requestId = UUID.randomUUID().toString()
        ctx.attribute("requestId", requestId)
        ctx.header("X-Request-ID", requestId)
    }

    private fun logRequest(ctx: Context) {
        val requestId = ctx.attribute<String>("requestId")
        logger.info("→ {} {} [{}] from {}", ctx.method(), ctx.path(), requestId, ctx.ip())
    }

    private fun logResponse(ctx: Context) {
        val requestId = ctx.attribute<String>("requestId")
        logger.info("← {} {} [{}]", ctx.status(), ctx.path(), requestId)
    }

    private fun createJacksonMapper(): io.javalin.json.JsonMapper {
        val objectMapper = com.fasterxml.jackson.module.kotlin.jacksonObjectMapper()
            .configure(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
            .configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .registerModule(com.fasterxml.jackson.datatype.jsr310.JavaTimeModule())

        return object : io.javalin.json.JsonMapper {
            override fun toJsonString(obj: Any, type: java.lang.reflect.Type): String {
                return objectMapper.writeValueAsString(obj)
            }

            override fun <T : Any> fromJsonString(json: String, targetType: java.lang.reflect.Type): T {
                return objectMapper.readValue(json, objectMapper.constructType(targetType))
            }
        }
    }
}
