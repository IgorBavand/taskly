package app.security

import org.slf4j.LoggerFactory

/**
 * Logger de auditoria para eventos de segurança.
 * Registra tentativas de autenticação, acessos negados e ações sensíveis.
 */
object AuditLogger {

    private val logger = LoggerFactory.getLogger("AUDIT")

    fun logAuthSuccess(email: String, ip: String) {
        logger.info("AUTH_SUCCESS | email={} | ip={}", email, ip)
    }

    fun logAuthFailure(email: String, ip: String, reason: String) {
        logger.warn("AUTH_FAILURE | email={} | ip={} | reason={}", email, ip, reason)
    }

    fun logRegistration(email: String, ip: String) {
        logger.info("USER_REGISTERED | email={} | ip={}", email, ip)
    }

    fun logTokenRefresh(userId: Long, ip: String) {
        logger.info("TOKEN_REFRESHED | userId={} | ip={}", userId, ip)
    }

    fun logAccessDenied(userId: Long?, resource: String, ip: String, reason: String) {
        logger.warn("ACCESS_DENIED | userId={} | resource={} | ip={} | reason={}", userId, resource, ip, reason)
    }

    fun logSensitiveAction(userId: Long, action: String, resource: String, ip: String) {
        logger.info("SENSITIVE_ACTION | userId={} | action={} | resource={} | ip={}", userId, action, resource, ip)
    }

    fun logPasswordChange(userId: Long, ip: String) {
        logger.info("PASSWORD_CHANGED | userId={} | ip={}", userId, ip)
    }

    fun logAccountDeactivation(userId: Long, deactivatedBy: Long, ip: String) {
        logger.warn("ACCOUNT_DEACTIVATED | userId={} | by={} | ip={}", userId, deactivatedBy, ip)
    }
}
