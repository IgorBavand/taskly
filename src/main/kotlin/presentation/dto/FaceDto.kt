package app.presentation.dto

data class FaceRegistrationRequest(
    val image: String
)

data class FaceVerificationRequest(
    val image: String
)

data class FaceVerificationResponse(
    val success: Boolean,
    val token: String? = null,
    val expiresAt: String? = null,
    val message: String
)

data class FaceValidationStatusResponse(
    val hasValidToken: Boolean,
    val expiresAt: String? = null
)
