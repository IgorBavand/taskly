package app.application.service

import app.domain.entity.FaceValidationToken
import app.domain.exception.InvalidCredentialsException
import app.domain.exception.ValidationException
import app.domain.repository.FaceValidationTokenRepository
import app.domain.repository.UserRepository
import app.infrastructure.external.DeepFaceService
import org.slf4j.LoggerFactory

/**
 * Serviço de aplicação para validação facial usando DeepFace.
 *
 * Fluxo de validação facial:
 * 1. Usuário registra rosto (primeira vez) → extrai embedding e salva no banco
 * 2. Usuário quer criar tarefa → verifica rosto atual contra embedding salvo
 * 3. Se verificado → emite token de 2 minutos
 * 4. Frontend usa token para criar tarefa
 *
 * Vantagens sobre CompreFace:
 * - Compatível com Apple Silicon (ARM64)
 * - Armazena embeddings ao invés de imagens (privacidade + economia)
 * - Código aberto sem restrições comerciais
 */
class FaceValidationService(
    private val deepFaceService: DeepFaceService,
    private val tokenRepository: FaceValidationTokenRepository,
    private val userRepository: UserRepository
) {

    private val logger = LoggerFactory.getLogger(FaceValidationService::class.java)

    /**
     * Registra o rosto do usuário pela primeira vez.
     *
     * Extrai o embedding facial e armazena no banco de dados.
     * Não armazena a imagem por questões de privacidade e espaço.
     *
     * @param userId ID do usuário
     * @param imageBase64 Imagem do rosto em Base64
     */
    fun registerUserFace(userId: Long, imageBase64: String) {
        logger.info("Registrando rosto para usuário $userId")

        val user = userRepository.findById(userId)
            ?: throw ValidationException("Usuário não encontrado")

        // Verificar se já tem rosto registrado
        if (user.faceEmbedding != null) {
            logger.warn("Usuário $userId já possui rosto registrado. Substituindo...")
        }

        // Extrair embedding
        val embeddingResult = deepFaceService.extractEmbedding(imageBase64)
            ?: throw ValidationException("Falha ao extrair características faciais. Certifique-se de que há um rosto visível na imagem.")

        logger.info("Embedding extraído: model=${embeddingResult.model}, dimensions=${embeddingResult.dimensions}")

        // Salvar embedding no usuário
        // Nota: Necessário adicionar campo faceEmbedding na entidade User
        val updatedUser = user.copy(
            faceEmbedding = embeddingResult.embedding.joinToString(",")
        )

        userRepository.update(updatedUser)

        logger.info("Rosto registrado com sucesso para usuário $userId")
    }

    /**
     * Verifica o rosto do usuário e emite um token de validação.
     *
     * Token é válido por 2 minutos e permite criar tarefas.
     * Cada verificação invalida tokens anteriores.
     *
     * @param userId ID do usuário
     * @param imageBase64 Imagem do rosto atual em Base64
     * @return Token de validação facial
     */
    fun verifyFaceAndIssueToken(userId: Long, imageBase64: String): FaceValidationToken {
        logger.info("Verificando rosto para usuário $userId")

        val user = userRepository.findById(userId)
            ?: throw ValidationException("Usuário não encontrado")

        // Verificar se usuário tem rosto registrado
        if (user.faceEmbedding == null) {
            throw ValidationException("Usuário não possui rosto registrado. Registre primeiro através do endpoint /face/register")
        }

        // Converter embedding string para lista
        val storedEmbedding = user.faceEmbedding!!.split(",").map { it.toDouble() }

        // Verificar rosto contra embedding armazenado
        val result = deepFaceService.verifyFaceWithEmbedding(imageBase64, storedEmbedding)
            ?: throw ValidationException("Falha ao verificar rosto. Tente novamente ou certifique-se de que há um rosto visível.")

        if (!result.verified) {
            logger.warn("Verificação facial falhou para usuário $userId: distance=${result.distance}, threshold=${result.threshold}")
            throw InvalidCredentialsException("Rosto não reconhecido. Similarity: ${String.format("%.2f", result.similarity)}%")
        }

        logger.info("Rosto verificado: distance=${result.distance}, similarity=${result.similarity}%, threshold=${result.threshold}")

        // Invalidar tokens anteriores do usuário
        tokenRepository.deleteByUserId(userId)

        // Criar novo token de 2 minutos
        val token = FaceValidationToken.create(userId)
        val savedToken = tokenRepository.save(token)

        logger.info("Token emitido para usuário $userId, expira em: ${savedToken.expiresAt}")

        return savedToken
    }

    /**
     * Valida se um token de validação facial é válido.
     *
     * Verifica:
     * - Token existe
     * - Pertence ao usuário correto
     * - Não expirou
     * - Não foi usado
     *
     * @param token Token a validar
     * @param userId ID do usuário
     * @return true se válido
     */
    fun validateToken(token: String, userId: Long): Boolean {
        val faceToken = tokenRepository.findByToken(token) ?: run {
            logger.warn("Token não encontrado")
            return false
        }

        if (faceToken.userId != userId) {
            logger.warn("Token pertence a outro usuário: expected=$userId, actual=${faceToken.userId}")
            return false
        }

        if (!faceToken.isValid()) {
            logger.warn("Token inválido ou expirado")
            return false
        }

        logger.debug("Token validado com sucesso para usuário $userId")
        return true
    }

    /**
     * Consome (marca como usado) um token de validação.
     *
     * Previne replay attacks.
     *
     * @param token Token a consumir
     */
    fun consumeToken(token: String) {
        val faceToken = tokenRepository.findByToken(token) ?: run {
            logger.warn("Tentativa de consumir token inexistente: $token")
            return
        }

        val usedToken = faceToken.markAsUsed()
        tokenRepository.update(usedToken)

        logger.debug("Token consumido: $token para usuário ${faceToken.userId}")
    }

    /**
     * Verifica se usuário tem token válido.
     *
     * @param userId ID do usuário
     * @return Informações sobre status da validação
     */
    fun hasValidToken(userId: Long): Pair<Boolean, String?> {
        val validToken = tokenRepository.findValidTokenByUserId(userId)

        return if (validToken != null) {
            Pair(true, validToken.expiresAt.toString())
        } else {
            Pair(false, null)
        }
    }

    /**
     * Remove o rosto registrado do usuário.
     *
     * Útil para:
     * - Usuário quer refazer o cadastro facial
     * - Conta deletada
     * - Requisição de privacidade (LGPD/GDPR)
     *
     * @param userId ID do usuário
     */
    fun deleteUserFace(userId: Long) {
        logger.info("Removendo rosto do usuário $userId")

        val user = userRepository.findById(userId)
            ?: throw ValidationException("Usuário não encontrado")

        if (user.faceEmbedding == null) {
            logger.warn("Usuário $userId não possui rosto registrado")
            return
        }

        // Remover embedding
        val updatedUser = user.copy(faceEmbedding = null)
        userRepository.update(updatedUser)

        // Invalidar tokens
        tokenRepository.deleteByUserId(userId)

        logger.info("Rosto removido com sucesso do usuário $userId")
    }

    /**
     * Limpa tokens expirados do banco de dados.
     *
     * Deve ser executado periodicamente por um scheduler.
     */
    fun cleanupExpiredTokens() {
        logger.info("Limpando tokens expirados...")
        val deletedCount = tokenRepository.deleteExpired()
        logger.info("$deletedCount tokens expirados removidos")
    }
}
