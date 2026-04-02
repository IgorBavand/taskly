package app.domain.exception

/**
 * Exceções de domínio da aplicação.
 * Seguem o padrão de DDD, representando erros de regras de negócio.
 */

/**
 * Exceção base para erros de domínio.
 * Todas as exceções de domínio devem herdar desta classe.
 */
abstract class DomainException(
    message: String,
    cause: Throwable? = null
) : RuntimeException(message, cause)

/**
 * Exceção base para recursos não encontrados (404)
 */
abstract class NotFoundException(
    message: String,
    cause: Throwable? = null
) : DomainException(message, cause)

/**
 * Exceção base para acesso negado (403)
 */
open class AccessDeniedException(
    message: String,
    cause: Throwable? = null
) : DomainException(message, cause)

/**
 * Exceção base para dados inválidos (400)
 */
open class ValidationException(
    message: String,
    cause: Throwable? = null
) : DomainException(message, cause)

/**
 * Exceção base para erros de autenticação (401)
 */
abstract class AuthenticationException(
    message: String,
    cause: Throwable? = null
) : DomainException(message, cause)

// ============================================================================
// TODO EXCEPTIONS
// ============================================================================

/**
 * Todo não encontrado
 */
class TodoNotFoundException(
    message: String = "Todo não encontrado"
) : NotFoundException(message)

/**
 * Acesso ao todo negado (não pertence ao usuário)
 */
class TodoAccessDeniedException(
    message: String = "Você não tem permissão para acessar este todo"
) : AccessDeniedException(message)

/**
 * Permissão insuficiente (falta role necessária)
 */
class InsufficientPermissionsException(
    requiredRole: String
) : AccessDeniedException("Permissão insuficiente. Necessária role: $requiredRole")

// ============================================================================
// USER EXCEPTIONS
// ============================================================================

/**
 * Usuário não encontrado
 */
class UserNotFoundException(
    message: String = "Usuário não encontrado"
) : NotFoundException(message)

/**
 * Email já cadastrado
 */
class EmailAlreadyExistsException(
    email: String
) : ValidationException("Email já cadastrado: $email")

/**
 * Conta desativada
 */
class AccountDisabledException(
    message: String = "Conta desativada"
) : AuthenticationException(message)

// ============================================================================
// AUTH EXCEPTIONS
// ============================================================================

/**
 * Credenciais inválidas (email ou senha incorretos)
 */
class InvalidCredentialsException(
    message: String = "Credenciais inválidas"
) : AuthenticationException(message)

/**
 * Refresh token inválido ou expirado
 */
class InvalidRefreshTokenException(
    message: String = "Refresh token inválido ou expirado"
) : AuthenticationException(message)

/**
 * Token JWT inválido ou expirado
 */
class InvalidTokenException(
    message: String = "Token inválido ou expirado"
) : AuthenticationException(message)

// ============================================================================
// VALIDATION EXCEPTIONS
// ============================================================================

/**
 * Senha inválida (não atende critérios de segurança)
 */
class InvalidPasswordException(
    message: String
) : ValidationException(message)

/**
 * Email inválido
 */
class InvalidEmailException(
    email: String
) : ValidationException("Email inválido: $email")

/**
 * Dados inválidos (genérico)
 */
class InvalidDataException(
    message: String
) : ValidationException(message)
