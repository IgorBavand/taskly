package app.infrastructure.external

import app.config.EnvConfig
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import org.slf4j.LoggerFactory

/**
 * Serviço de integração com DeepFace Microservice.
 *
 * Responsável por:
 * - Extrair embeddings faciais
 * - Verificar se dois rostos correspondem
 * - Integração com microserviço Python via REST
 *
 * Vantagens sobre CompreFace:
 * - Compatível com Apple Silicon (ARM64)
 * - Uso comercial livre
 * - Armazenamento de embeddings ao invés de imagens
 */
class DeepFaceService {

    private val logger = LoggerFactory.getLogger(DeepFaceService::class.java)
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .writeTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .build()
    private val mapper = jacksonObjectMapper()

    private val deepFaceUrl = EnvConfig.DEEPFACE_URL
    private val verificationThreshold = EnvConfig.DEEPFACE_THRESHOLD

    /**
     * Resultado da verificação facial
     */
    data class VerificationResult(
        val verified: Boolean,
        val distance: Double,
        val similarity: Double,
        val threshold: Double,
        val model: String
    )

    /**
     * Resultado da extração de embedding
     */
    data class EmbeddingResult(
        val embedding: List<Double>,
        val model: String,
        val dimensions: Int
    )

    /**
     * Request para verificação facial
     */
    private data class VerifyRequest(
        val img1_base64: String,
        val img2_base64: String? = null,
        val embedding: List<Double>? = null,
        val threshold: Double? = null
    )

    /**
     * Request para extração de embedding
     */
    private data class EmbeddingRequest(
        val img_base64: String
    )

    /**
     * Extrai o embedding facial de uma imagem.
     *
     * Embedding é um vetor numérico que representa as características únicas do rosto.
     * Armazenamos este vetor ao invés da imagem para:
     * - Economia de espaço
     * - Privacidade (não armazena a foto)
     * - Performance (comparação mais rápida)
     *
     * @param imageBase64 Imagem do rosto em Base64
     * @return EmbeddingResult com o vetor de características
     */
    fun extractEmbedding(imageBase64: String): EmbeddingResult? {
        return try {
            logger.info("Extraindo embedding facial via DeepFace")

            // Limpar prefixo data URI se presente
            val cleanBase64 = imageBase64.removePrefix("data:image/jpeg;base64,")
                .removePrefix("data:image/png;base64,")

            val requestBody = mapper.writeValueAsString(
                EmbeddingRequest(img_base64 = cleanBase64)
            )

            val request = Request.Builder()
                .url("$deepFaceUrl/face/embedding")
                .addHeader("Content-Type", "application/json")
                .post(RequestBody.create("application/json".toMediaType(), requestBody))
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val errorBody = response.body?.string()
                    logger.error("Erro ao extrair embedding: ${response.code} - $errorBody")
                    return null
                }

                val responseBody = response.body?.string() ?: return null
                val jsonResponse: Map<String, Any> = mapper.readValue(responseBody)

                @Suppress("UNCHECKED_CAST")
                val embedding = (jsonResponse["embedding"] as? List<Number>)?.map { it.toDouble() }
                    ?: return null

                val model = jsonResponse["model"] as? String ?: "unknown"
                val dimensions = (jsonResponse["dimensions"] as? Number)?.toInt() ?: embedding.size

                logger.info("Embedding extraído com sucesso: model=$model, dimensions=$dimensions")

                EmbeddingResult(
                    embedding = embedding,
                    model = model,
                    dimensions = dimensions
                )
            }
        } catch (e: Exception) {
            logger.error("Erro ao extrair embedding: ${e.message}", e)
            null
        }
    }

    /**
     * Verifica se um rosto corresponde a um embedding armazenado.
     *
     * @param imageBase64 Imagem do rosto atual em Base64
     * @param storedEmbedding Embedding armazenado no banco de dados
     * @param threshold Limiar de distância (menor = mais estrito). Padrão do .env
     * @return VerificationResult com resultado da verificação
     */
    fun verifyFaceWithEmbedding(
        imageBase64: String,
        storedEmbedding: List<Double>,
        threshold: Double? = null
    ): VerificationResult? {
        return try {
            logger.info("Verificando rosto contra embedding armazenado")

            // Limpar prefixo data URI
            val cleanBase64 = imageBase64.removePrefix("data:image/jpeg;base64,")
                .removePrefix("data:image/png;base64,")

            val effectiveThreshold = threshold ?: verificationThreshold

            val requestBody = mapper.writeValueAsString(
                VerifyRequest(
                    img1_base64 = cleanBase64,
                    embedding = storedEmbedding,
                    threshold = effectiveThreshold
                )
            )

            val request = Request.Builder()
                .url("$deepFaceUrl/face/verify")
                .addHeader("Content-Type", "application/json")
                .post(RequestBody.create("application/json".toMediaType(), requestBody))
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val errorBody = response.body?.string()
                    logger.error("Erro ao verificar rosto: ${response.code} - $errorBody")
                    return null
                }

                val responseBody = response.body?.string() ?: return null
                val jsonResponse: Map<String, Any> = mapper.readValue(responseBody)

                val verified = jsonResponse["verified"] as? Boolean ?: false
                val distance = (jsonResponse["distance"] as? Number)?.toDouble() ?: 1.0
                val similarity = (jsonResponse["similarity"] as? Number)?.toDouble() ?: 0.0
                val responseThreshold = (jsonResponse["threshold"] as? Number)?.toDouble() ?: effectiveThreshold
                val model = jsonResponse["model"] as? String ?: "unknown"

                logger.info("Verificação concluída: verified=$verified, distance=$distance, similarity=$similarity%")

                VerificationResult(
                    verified = verified,
                    distance = distance,
                    similarity = similarity,
                    threshold = responseThreshold,
                    model = model
                )
            }
        } catch (e: Exception) {
            logger.error("Erro ao verificar rosto: ${e.message}", e)
            null
        }
    }

    /**
     * Verifica se duas imagens são do mesmo rosto.
     *
     * Útil para validações pontuais sem armazenar embedding.
     *
     * @param imageBase64_1 Primeira imagem em Base64
     * @param imageBase64_2 Segunda imagem em Base64
     * @param threshold Limiar de distância. Padrão do .env
     * @return VerificationResult com resultado da verificação
     */
    fun verifyFaces(
        imageBase64_1: String,
        imageBase64_2: String,
        threshold: Double? = null
    ): VerificationResult? {
        return try {
            logger.info("Verificando duas imagens faciais")

            val cleanBase64_1 = imageBase64_1.removePrefix("data:image/jpeg;base64,")
                .removePrefix("data:image/png;base64,")
            val cleanBase64_2 = imageBase64_2.removePrefix("data:image/jpeg;base64,")
                .removePrefix("data:image/png;base64,")

            val effectiveThreshold = threshold ?: verificationThreshold

            val requestBody = mapper.writeValueAsString(
                VerifyRequest(
                    img1_base64 = cleanBase64_1,
                    img2_base64 = cleanBase64_2,
                    threshold = effectiveThreshold
                )
            )

            val request = Request.Builder()
                .url("$deepFaceUrl/face/verify")
                .addHeader("Content-Type", "application/json")
                .post(RequestBody.create("application/json".toMediaType(), requestBody))
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val errorBody = response.body?.string()
                    logger.error("Erro ao verificar rostos: ${response.code} - $errorBody")
                    return null
                }

                val responseBody = response.body?.string() ?: return null
                val jsonResponse: Map<String, Any> = mapper.readValue(responseBody)

                val verified = jsonResponse["verified"] as? Boolean ?: false
                val distance = (jsonResponse["distance"] as? Number)?.toDouble() ?: 1.0
                val similarity = (jsonResponse["similarity"] as? Number)?.toDouble() ?: 0.0
                val responseThreshold = (jsonResponse["threshold"] as? Number)?.toDouble() ?: effectiveThreshold
                val model = jsonResponse["model"] as? String ?: "unknown"

                logger.info("Verificação concluída: verified=$verified, distance=$distance, similarity=$similarity%")

                VerificationResult(
                    verified = verified,
                    distance = distance,
                    similarity = similarity,
                    threshold = responseThreshold,
                    model = model
                )
            }
        } catch (e: Exception) {
            logger.error("Erro ao verificar rostos: ${e.message}", e)
            null
        }
    }

    /**
     * Verifica se o microserviço DeepFace está saudável.
     *
     * @return true se o serviço está disponível
     */
    fun healthCheck(): Boolean {
        return try {
            val request = Request.Builder()
                .url("$deepFaceUrl/health")
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                val healthy = response.isSuccessful
                if (healthy) {
                    logger.info("DeepFace service is healthy")
                } else {
                    logger.warn("DeepFace service returned: ${response.code}")
                }
                healthy
            }
        } catch (e: Exception) {
            logger.error("DeepFace service health check failed: ${e.message}")
            false
        }
    }
}
