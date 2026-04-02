# 🔐 Implementação de Validação Facial - Taskly API

## 📋 Índice
1. [Arquitetura](#arquitetura)
2. [Backend - Código Completo](#backend)
3. [Frontend Angular - Código Completo](#frontend)
4. [Configuração e Setup](#configuração)
5. [Exemplos de Uso](#exemplos)
6. [Segurança](#segurança)
7. [Melhorias Futuras](#melhorias)

---

## 🏗️ Arquitetura {#arquitetura}

### Backend (Kotlin/Javalin)
```
app/
├── domain/
│   ├── entity/
│   │   └── FaceValidationToken.kt ✅ (criado)
│   └── repository/
│       └── FaceValidationTokenRepository.kt ✅ (criado)
├── application/
│   └── service/
│       └── FaceValidationService.kt (continuar)
├── infrastructure/
│   ├── external/
│   │   └── CompreFaceService.kt ✅ (criado)
│   └── persistence/
│       ├── entity/
│       │   └── FaceValidationTokenEntity.kt ✅ (criado)
│       └── repository/
│           └── JpaFaceValidationTokenRepository.kt ✅ (criado)
├── presentation/
│   ├── controller/
│   │   └── FaceController.kt (continuar)
│   └── dto/
│       └── FaceDto.kt (continuar)
└── security/
    └── FaceValidationMiddleware.kt (continuar)
```

### Frontend (Angular)
```
src/
├── app/
│   ├── features/
│   │   ├── face-validation/
│   │   │   ├── components/
│   │   │   │   ├── face-capture/
│   │   │   │   │   ├── face-capture.component.ts
│   │   │   │   │   ├── face-capture.component.html
│   │   │   │   │   └── face-capture.component.scss
│   │   │   │   └── face-registration/
│   │   │   │       ├── face-registration.component.ts
│   │   │   │       ├── face-registration.component.html
│   │   │   │       └── face-registration.component.scss
│   │   │   ├── services/
│   │   │   │   └── face-validation.service.ts
│   │   │   └── guards/
│   │   │       └── face-validation.guard.ts
│   │   └── todos/
│   │       ├── components/
│   │       │   └── todo-create/
│   │       │       ├── todo-create.component.ts
│   │       │       ├── todo-create.component.html
│   │       │       └── todo-create.component.scss
│   │       └── services/
│   │           └── todo.service.ts
│   └── core/
│       └── interceptors/
│           └── face-token.interceptor.ts
```

---

## 💻 Backend - Código Completo {#backend}

### 7️⃣ FaceValidationService.kt

```kotlin
package app.application.service

import app.domain.entity.FaceValidationToken
import app.domain.exception.InvalidCredentialsException
import app.domain.exception.ValidationException
import app.domain.repository.FaceValidationTokenRepository
import app.domain.repository.UserRepository
import app.infrastructure.external.CompreFaceService
import org.slf4j.LoggerFactory

/**
 * Serviço de aplicação para validação facial.
 *
 * Orquestra a lógica de negócio para:
 * - Registrar rostos de usuários
 * - Verificar rostos e emitir tokens
 * - Validar tokens de validação facial
 */
class FaceValidationService(
    private val compreFaceService: CompreFaceService,
    private val tokenRepository: FaceValidationTokenRepository,
    private val userRepository: UserRepository
) {

    private val logger = LoggerFactory.getLogger(FaceValidationService::class.java)

    /**
     * Registra o rosto de um usuário no CompreFace.
     *
     * Deve ser chamado uma única vez, geralmente durante o cadastro
     * ou em uma tela de "Ativar Validação Facial".
     *
     * @param userId ID do usuário
     * @param imageBase64 Imagem do rosto em Base64
     * @throws ValidationException se o registro falhar
     */
    fun registerUserFace(userId: Long, imageBase64: String) {
        logger.info("Registrando rosto para usuário $userId")

        // Verificar se usuário existe
        val user = userRepository.findById(userId)
            ?: throw ValidationException("Usuário não encontrado")

        // Registrar no CompreFace
        val success = compreFaceService.registerFace(userId, imageBase64)

        if (!success) {
            throw ValidationException("Falha ao registrar rosto. Certifique-se de que há um rosto visível na imagem.")
        }

        logger.info("Rosto registrado com sucesso para usuário $userId")
    }

    /**
     * Verifica o rosto do usuário e emite um token de validação facial.
     *
     * Fluxo:
     * 1. Verifica se o rosto na imagem pertence ao usuário
     * 2. Se verificado, cria um token temporário (2 minutos)
     * 3. Retorna o token para uso em operações protegidas
     *
     * @param userId ID do usuário
     * @param imageBase64 Imagem do rosto em Base64
     * @return FaceValidationToken
     * @throws InvalidCredentialsException se a verificação falhar
     */
    fun verifyFaceAndIssueToken(userId: Long, imageBase64: String): FaceValidationToken {
        logger.info("Verificando rosto para usuário $userId")

        // Verificar se usuário existe
        val user = userRepository.findById(userId)
            ?: throw ValidationException("Usuário não encontrado")

        // Verificar rosto no CompreFace
        val result = compreFaceService.verifyFace(userId, imageBase64, threshold = 0.85)

        if (!result.verified) {
            logger.warn("Verificação facial falhou para usuário $userId. Similarity: ${result.similarity}")
            throw InvalidCredentialsException("Rosto não reconhecido. Tente novamente.")
        }

        logger.info("Rosto verificado com sucesso para usuário $userId. Similarity: ${result.similarity}")

        // Invalidar tokens anteriores deste usuário (prevenir acúmulo)
        tokenRepository.deleteByUserId(userId)

        // Criar e salvar novo token
        val token = FaceValidationToken.create(userId)
        val savedToken = tokenRepository.save(token)

        logger.info("Token de validação facial emitido para usuário $userId: ${savedToken.token}")

        return savedToken
    }

    /**
     * Valida um token de validação facial.
     *
     * @param token String do token
     * @param userId ID do usuário que está tentando usar o token
     * @return true se válido
     */
    fun validateToken(token: String, userId: Long): Boolean {
        logger.debug("Validando token de validação facial para usuário $userId")

        val faceToken = tokenRepository.findByToken(token) ?: return false

        // Verificar se o token pertence ao usuário
        if (faceToken.userId != userId) {
            logger.warn("Tentativa de uso de token de outro usuário: token.userId=${faceToken.userId}, userId=$userId")
            return false
        }

        // Verificar se o token é válido (não expirado e não usado)
        if (!faceToken.isValid()) {
            logger.debug("Token inválido ou expirado para usuário $userId")
            return false
        }

        logger.info("Token de validação facial válido para usuário $userId")
        return true
    }

    /**
     * Consome um token de validação facial, marcando-o como usado.
     *
     * Deve ser chamado após validação bem-sucedida ao criar uma tarefa.
     *
     * @param token String do token
     */
    fun consumeToken(token: String) {
        val faceToken = tokenRepository.findByToken(token) ?: return

        val usedToken = faceToken.markAsUsed()
        tokenRepository.update(usedToken)

        logger.info("Token de validação facial consumido: ${token.take(8)}...")
    }

    /**
     * Verifica se o usuário tem um token válido ativo.
     *
     * @param userId ID do usuário
     * @return true se existe token válido
     */
    fun hasValidToken(userId: Long): Boolean {
        val token = tokenRepository.findValidTokenByUserId(userId)
        return token != null && token.isValid()
    }

    /**
     * Remove o rosto de um usuário do CompreFace.
     *
     * @param userId ID do usuário
     */
    fun deleteUserFace(userId: Long) {
        logger.info("Removendo rosto do usuário $userId")

        compreFaceService.deleteFace(userId)
        tokenRepository.deleteByUserId(userId)

        logger.info("Rosto removido com sucesso para usuário $userId")
    }

    /**
     * Limpa tokens expirados do banco de dados.
     *
     * Deve ser executado periodicamente (ex: cron job).
     */
    fun cleanupExpiredTokens() {
        logger.info("Limpando tokens de validação facial expirados")
        tokenRepository.deleteExpired()
    }
}
```

### 8️⃣ DTOs - FaceDto.kt

```kotlin
package app.presentation.dto

/**
 * DTOs para operações de validação facial
 */

/**
 * Request para registrar rosto
 */
data class FaceRegistrationRequest(
    val image: String // Base64
)

/**
 * Request para verificar rosto
 */
data class FaceVerificationRequest(
    val image: String // Base64
)

/**
 * Response de verificação facial
 */
data class FaceVerificationResponse(
    val success: Boolean,
    val token: String? = null,
    val expiresAt: String? = null,
    val message: String
)

/**
 * Response de status de validação facial
 */
data class FaceValidationStatusResponse(
    val hasValidToken: Boolean,
    val expiresAt: String? = null
)
```

Continua no próximo arquivo...
