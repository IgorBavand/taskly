package app.security

import io.javalin.http.Context
import io.javalin.http.Handler
import app.config.EnvConfig
import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentHashMap
import java.time.Instant

/**
 * Middleware de Rate Limiting para proteção contra brute force.
 * Implementa sliding window algorithm.
 */
open class RateLimiter(
    private val maxRequests: Int = EnvConfig.RATE_LIMIT_MAX_REQUESTS,
    private val windowSeconds: Long = EnvConfig.RATE_LIMIT_WINDOW_SECONDS
) : Handler {

    private val logger = LoggerFactory.getLogger(RateLimiter::class.java)

    private data class ClientInfo(
        var count: Int = 0,
        var windowStart: Instant = Instant.now()
    )

    private val clients = ConcurrentHashMap<String, ClientInfo>()

    override fun handle(ctx: Context) {
        val clientId = getClientIdentifier(ctx)
        val now = Instant.now()

        val info = clients.compute(clientId) { _, existing ->
            val current = existing ?: ClientInfo()

            // Reset se a janela expirou
            if (now.epochSecond - current.windowStart.epochSecond > windowSeconds) {
                ClientInfo(count = 1, windowStart = now)
            } else {
                current.count++
                current
            }
        }!!

        // Headers informativos
        ctx.header("X-RateLimit-Limit", maxRequests.toString())
        ctx.header("X-RateLimit-Remaining", maxOf(0, maxRequests - info.count).toString())
        ctx.header("X-RateLimit-Reset", (info.windowStart.epochSecond + windowSeconds).toString())

        if (info.count > maxRequests) {
            logger.warn("Rate limit excedido. Client: $clientId, Path: ${ctx.path()}")
            ctx.status(429).json(
                mapOf(
                    "error" to "Muitas requisições. Tente novamente em alguns segundos.",
                    "retryAfter" to windowSeconds
                )
            )
            return
        }

        // Cleanup periódico (remove entradas antigas)
        if (clients.size > 10000) {
            cleanupOldEntries(now)
        }
    }

    private fun getClientIdentifier(ctx: Context): String {
        // Prioriza X-Forwarded-For se atrás de proxy
        val forwardedFor = ctx.header("X-Forwarded-For")
        return forwardedFor?.split(",")?.firstOrNull()?.trim() ?: ctx.ip()
    }

    private fun cleanupOldEntries(now: Instant) {
        clients.entries.removeIf { (_, info) ->
            now.epochSecond - info.windowStart.epochSecond > windowSeconds * 2
        }
    }
}

/**
 * Rate limiter específico para endpoints de autenticação (mais restritivo)
 */
class AuthRateLimiter : RateLimiter(
    maxRequests = EnvConfig.AUTH_RATE_LIMIT_MAX_REQUESTS,
    windowSeconds = EnvConfig.RATE_LIMIT_WINDOW_SECONDS
)
