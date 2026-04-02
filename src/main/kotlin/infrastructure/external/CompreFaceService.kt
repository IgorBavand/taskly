package app.infrastructure.external

import app.config.EnvConfig
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.asRequestBody
import org.slf4j.LoggerFactory
import java.io.File
import java.util.Base64

/**
 * Serviço de integração com CompreFace API.
 *
 * Responsável por:
 * - Registrar rostos de usuários
 * - Verificar se um rosto pertence a um usuário específico
 * - Reconhecer rostos e retornar o usuário correspondente
 */
class CompreFaceService {

    private val logger = LoggerFactory.getLogger(CompreFaceService::class.java)
    private val client = OkHttpClient()
    private val mapper = jacksonObjectMapper()

    private val baseUrl = EnvConfig.COMPREFACE_URL
    private val apiKey = EnvConfig.COMPREFACE_API_KEY

    /**
     * Resultado da verificação facial
     */
    data class VerificationResult(
        val verified: Boolean,
        val similarity: Double,
        val box: Map<String, Any>? = null
    )

    /**
     * Resultado do reconhecimento facial
     */
    data class RecognitionResult(
        val userId: String,
        val similarity: Double,
        val box: Map<String, Any>? = null
    )

    /**
     * Registra um novo rosto para um usuário no CompreFace.
     *
     * @param userId ID do usuário (subject no CompreFace)
     * @param imageBase64 Imagem do rosto em Base64
     * @return true se registrado com sucesso
     */
    fun registerFace(userId: Long, imageBase64: String): Boolean {
        return try {
            logger.info("Registrando rosto para usuário $userId no CompreFace")

            val imageData = Base64.getDecoder().decode(imageBase64.removePrefix("data:image/jpeg;base64,"))
            val tempFile = File.createTempFile("face_", ".jpg")
            tempFile.writeBytes(imageData)

            val requestBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart(
                    "file",
                    "face.jpg",
                    tempFile.asRequestBody("image/jpeg".toMediaType())
                )
                .build()

            val request = Request.Builder()
                .url("$baseUrl/api/v1/recognition/faces?subject=$userId")
                .addHeader("x-api-key", apiKey)
                .post(requestBody)
                .build()

            client.newCall(request).execute().use { response ->
                tempFile.delete()

                if (!response.isSuccessful) {
                    logger.error("Erro ao registrar rosto: ${response.code} - ${response.body?.string()}")
                    return false
                }

                logger.info("Rosto registrado com sucesso para usuário $userId")
                true
            }
        } catch (e: Exception) {
            logger.error("Erro ao registrar rosto: ${e.message}", e)
            false
        }
    }

    /**
     * Verifica se um rosto corresponde a um usuário específico.
     *
     * @param userId ID do usuário a ser verificado
     * @param imageBase64 Imagem do rosto em Base64
     * @param threshold Limiar de similaridade (0.0 a 1.0). Padrão: 0.85
     * @return VerificationResult com resultado da verificação
     */
    fun verifyFace(userId: Long, imageBase64: String, threshold: Double = 0.85): VerificationResult {
        return try {
            logger.info("Verificando rosto para usuário $userId")

            val imageData = Base64.getDecoder().decode(imageBase64.removePrefix("data:image/jpeg;base64,"))
            val tempFile = File.createTempFile("face_", ".jpg")
            tempFile.writeBytes(imageData)

            val requestBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart(
                    "file",
                    "face.jpg",
                    tempFile.asRequestBody("image/jpeg".toMediaType())
                )
                .build()

            val request = Request.Builder()
                .url("$baseUrl/api/v1/recognition/faces/$userId/verify?limit=1&det_prob_threshold=0.8&face_plugins=age,gender,landmarks")
                .addHeader("x-api-key", apiKey)
                .post(requestBody)
                .build()

            client.newCall(request).execute().use { response ->
                tempFile.delete()

                if (!response.isSuccessful) {
                    logger.error("Erro ao verificar rosto: ${response.code} - ${response.body?.string()}")
                    return VerificationResult(verified = false, similarity = 0.0)
                }

                val responseBody = response.body?.string() ?: "{}"
                val jsonResponse: Map<String, Any> = mapper.readValue(responseBody)

                val result = jsonResponse["result"] as? List<Map<String, Any>>
                if (result.isNullOrEmpty()) {
                    logger.warn("Nenhum rosto detectado na imagem")
                    return VerificationResult(verified = false, similarity = 0.0)
                }

                val firstResult = result[0]
                val similarity = (firstResult["similarity"] as? Number)?.toDouble() ?: 0.0
                val box = firstResult["box"] as? Map<String, Any>

                val verified = similarity >= threshold

                logger.info("Verificação concluída: verified=$verified, similarity=$similarity")

                VerificationResult(
                    verified = verified,
                    similarity = similarity,
                    box = box
                )
            }
        } catch (e: Exception) {
            logger.error("Erro ao verificar rosto: ${e.message}", e)
            VerificationResult(verified = false, similarity = 0.0)
        }
    }

    /**
     * Reconhece um rosto e retorna o usuário correspondente.
     *
     * @param imageBase64 Imagem do rosto em Base64
     * @param threshold Limiar de similaridade (0.0 a 1.0). Padrão: 0.85
     * @return RecognitionResult ou null se não reconhecido
     */
    fun recognizeFace(imageBase64: String, threshold: Double = 0.85): RecognitionResult? {
        return try {
            logger.info("Reconhecendo rosto")

            val imageData = Base64.getDecoder().decode(imageBase64.removePrefix("data:image/jpeg;base64,"))
            val tempFile = File.createTempFile("face_", ".jpg")
            tempFile.writeBytes(imageData)

            val requestBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart(
                    "file",
                    "face.jpg",
                    tempFile.asRequestBody("image/jpeg".toMediaType())
                )
                .build()

            val request = Request.Builder()
                .url("$baseUrl/api/v1/recognition/recognize?limit=1&det_prob_threshold=0.8&face_plugins=age,gender,landmarks")
                .addHeader("x-api-key", apiKey)
                .post(requestBody)
                .build()

            client.newCall(request).execute().use { response ->
                tempFile.delete()

                if (!response.isSuccessful) {
                    logger.error("Erro ao reconhecer rosto: ${response.code} - ${response.body?.string()}")
                    return null
                }

                val responseBody = response.body?.string() ?: "{}"
                val jsonResponse: Map<String, Any> = mapper.readValue(responseBody)

                val result = jsonResponse["result"] as? List<Map<String, Any>>
                if (result.isNullOrEmpty()) {
                    logger.warn("Nenhum rosto reconhecido na imagem")
                    return null
                }

                val firstResult = result[0]
                val subjects = firstResult["subjects"] as? List<Map<String, Any>>
                if (subjects.isNullOrEmpty()) {
                    logger.warn("Nenhum subject encontrado")
                    return null
                }

                val subject = subjects[0]
                val userId = subject["subject"] as? String ?: return null
                val similarity = (subject["similarity"] as? Number)?.toDouble() ?: 0.0
                val box = firstResult["box"] as? Map<String, Any>

                if (similarity < threshold) {
                    logger.warn("Similarity abaixo do threshold: $similarity < $threshold")
                    return null
                }

                logger.info("Rosto reconhecido: userId=$userId, similarity=$similarity")

                RecognitionResult(
                    userId = userId,
                    similarity = similarity,
                    box = box
                )
            }
        } catch (e: Exception) {
            logger.error("Erro ao reconhecer rosto: ${e.message}", e)
            null
        }
    }

    /**
     * Remove um rosto de um usuário do CompreFace.
     *
     * @param userId ID do usuário
     * @return true se removido com sucesso
     */
    fun deleteFace(userId: Long): Boolean {
        return try {
            logger.info("Removendo rosto do usuário $userId do CompreFace")

            val request = Request.Builder()
                .url("$baseUrl/api/v1/recognition/faces?subject=$userId")
                .addHeader("x-api-key", apiKey)
                .delete()
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    logger.error("Erro ao remover rosto: ${response.code} - ${response.body?.string()}")
                    return false
                }

                logger.info("Rosto removido com sucesso do usuário $userId")
                true
            }
        } catch (e: Exception) {
            logger.error("Erro ao remover rosto: ${e.message}", e)
            false
        }
    }
}
