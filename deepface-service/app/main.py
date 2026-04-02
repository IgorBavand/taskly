"""
DeepFace Microservice - Face Verification API
Compatible with Apple Silicon (ARM64)
"""
import os
import base64
import tempfile
import logging
from pathlib import Path
from typing import Optional
from contextlib import asynccontextmanager

from fastapi import FastAPI, HTTPException, UploadFile, File
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel, Field
from PIL import Image
import io
import numpy as np

from deepface import DeepFace

# Configure logging
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s'
)
logger = logging.getLogger(__name__)


# ============================================================================
# CONFIGURATION
# ============================================================================

class Settings(BaseModel):
    """Application settings"""
    model_name: str = Field(default="Facenet512", description="DeepFace model to use")
    distance_metric: str = Field(default="cosine", description="Distance metric")
    detector_backend: str = Field(default="opencv", description="Face detector backend")
    verification_threshold: float = Field(default=0.4, description="Verification threshold")
    enforce_detection: bool = Field(default=True, description="Enforce face detection")

    class Config:
        env_prefix = "DEEPFACE_"


settings = Settings()


# ============================================================================
# MODELS
# ============================================================================

class FaceVerifyRequest(BaseModel):
    """Face verification request"""
    img1_base64: str = Field(..., description="First image in base64 format")
    img2_base64: Optional[str] = Field(None, description="Second image in base64 (optional if using embedding)")
    embedding: Optional[list[float]] = Field(None, description="Pre-computed embedding instead of img2")
    model_name: Optional[str] = Field(None, description="Override default model")
    threshold: Optional[float] = Field(None, description="Override default threshold")


class FaceVerifyResponse(BaseModel):
    """Face verification response"""
    verified: bool = Field(..., description="Whether faces match")
    distance: float = Field(..., description="Distance between faces")
    threshold: float = Field(..., description="Threshold used")
    model: str = Field(..., description="Model used")
    similarity: float = Field(..., description="Similarity percentage")


class FaceEmbeddingRequest(BaseModel):
    """Face embedding extraction request"""
    img_base64: str = Field(..., description="Image in base64 format")
    model_name: Optional[str] = Field(None, description="Override default model")


class FaceEmbeddingResponse(BaseModel):
    """Face embedding response"""
    embedding: list[float] = Field(..., description="Face embedding vector")
    model: str = Field(..., description="Model used")
    dimensions: int = Field(..., description="Embedding dimensions")


class HealthResponse(BaseModel):
    """Health check response"""
    status: str
    model: str
    detector: str
    version: str


# ============================================================================
# UTILITIES
# ============================================================================

def base64_to_image(base64_string: str) -> str:
    """
    Convert base64 string to temporary image file.

    Args:
        base64_string: Base64 encoded image (with or without data URI prefix)

    Returns:
        Path to temporary image file
    """
    try:
        # Remove data URI prefix if present
        if "base64," in base64_string:
            base64_string = base64_string.split("base64,")[1]

        # Decode base64
        image_data = base64.b64decode(base64_string)

        # Verify it's a valid image
        image = Image.open(io.BytesIO(image_data))

        # Save to temporary file
        temp_file = tempfile.NamedTemporaryFile(delete=False, suffix=".jpg")
        image.save(temp_file.name, format="JPEG")
        temp_file.close()

        return temp_file.name

    except Exception as e:
        logger.error(f"Error converting base64 to image: {e}")
        raise HTTPException(status_code=400, detail=f"Invalid image data: {str(e)}")


def cleanup_temp_file(filepath: str):
    """Delete temporary file"""
    try:
        if filepath and os.path.exists(filepath):
            os.remove(filepath)
    except Exception as e:
        logger.warning(f"Error deleting temp file {filepath}: {e}")


# ============================================================================
# LIFESPAN
# ============================================================================

@asynccontextmanager
async def lifespan(app: FastAPI):
    """Application lifespan - startup and shutdown"""
    logger.info("🚀 Starting DeepFace Service...")
    logger.info(f"📊 Model: {settings.model_name}")
    logger.info(f"🎯 Detector: {settings.detector_backend}")
    logger.info(f"📏 Distance Metric: {settings.distance_metric}")
    logger.info(f"⚖️  Threshold: {settings.verification_threshold}")

    # Pre-load model (optional, improves first request performance)
    try:
        logger.info("🔄 Pre-loading DeepFace model...")
        DeepFace.build_model(settings.model_name)
        logger.info("✅ Model pre-loaded successfully")
    except Exception as e:
        logger.warning(f"⚠️  Could not pre-load model: {e}")

    yield

    logger.info("🛑 Shutting down DeepFace Service...")


# ============================================================================
# APPLICATION
# ============================================================================

app = FastAPI(
    title="DeepFace Face Verification API",
    description="Microservice for face verification using DeepFace",
    version="1.0.0",
    lifespan=lifespan
)

# CORS configuration
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],  # Configure properly in production
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)


# ============================================================================
# ENDPOINTS
# ============================================================================

@app.get("/", response_model=dict)
async def root():
    """Root endpoint"""
    return {
        "service": "DeepFace Face Verification API",
        "version": "1.0.0",
        "status": "running"
    }


@app.get("/health", response_model=HealthResponse)
async def health_check():
    """Health check endpoint"""
    return HealthResponse(
        status="healthy",
        model=settings.model_name,
        detector=settings.detector_backend,
        version="1.0.0"
    )


@app.post("/face/verify", response_model=FaceVerifyResponse)
async def verify_faces(request: FaceVerifyRequest):
    """
    Verify if two faces match.

    Can verify:
    - Two images (img1_base64 + img2_base64)
    - Image against embedding (img1_base64 + embedding)
    """
    img1_path = None
    img2_path = None

    try:
        # Convert first image
        img1_path = base64_to_image(request.img1_base64)

        # Use provided model or default
        model = request.model_name or settings.model_name
        threshold = request.threshold or settings.verification_threshold

        # Verify against image or embedding
        if request.img2_base64:
            # Image to image verification
            img2_path = base64_to_image(request.img2_base64)

            result = DeepFace.verify(
                img1_path=img1_path,
                img2_path=img2_path,
                model_name=model,
                distance_metric=settings.distance_metric,
                detector_backend=settings.detector_backend,
                enforce_detection=settings.enforce_detection
            )

            distance = result["distance"]
            verified = result["verified"]

        elif request.embedding:
            # Image to embedding verification
            # Extract embedding from img1
            embedding_objs = DeepFace.represent(
                img_path=img1_path,
                model_name=model,
                detector_backend=settings.detector_backend,
                enforce_detection=settings.enforce_detection
            )

            if not embedding_objs:
                raise HTTPException(status_code=400, detail="No face detected in image")

            img1_embedding = embedding_objs[0]["embedding"]

            # Calculate distance using numpy (more compatible across DeepFace versions)
            embedding1 = np.array(img1_embedding)
            embedding2 = np.array(request.embedding)

            if settings.distance_metric == "cosine":
                # Cosine distance = 1 - cosine similarity
                dot_product = np.dot(embedding1, embedding2)
                norm1 = np.linalg.norm(embedding1)
                norm2 = np.linalg.norm(embedding2)
                cosine_similarity = dot_product / (norm1 * norm2)
                distance = 1 - cosine_similarity
            elif settings.distance_metric == "euclidean":
                # Euclidean distance
                distance = np.linalg.norm(embedding1 - embedding2)
            elif settings.distance_metric == "euclidean_l2":
                # L2 normalized Euclidean distance
                embedding1_norm = embedding1 / np.linalg.norm(embedding1)
                embedding2_norm = embedding2 / np.linalg.norm(embedding2)
                distance = np.linalg.norm(embedding1_norm - embedding2_norm)
            else:
                raise HTTPException(status_code=400, detail=f"Unsupported distance metric: {settings.distance_metric}")

            verified = distance <= threshold

        else:
            raise HTTPException(
                status_code=400,
                detail="Must provide either img2_base64 or embedding"
            )

        # Calculate similarity percentage
        similarity = max(0, min(100, (1 - distance) * 100))

        logger.info(f"Verification result: verified={verified}, distance={distance:.4f}, similarity={similarity:.2f}%")

        return FaceVerifyResponse(
            verified=verified,
            distance=float(distance),
            threshold=threshold,
            model=model,
            similarity=float(similarity)
        )

    except HTTPException:
        raise

    except Exception as e:
        logger.error(f"Error during face verification: {e}")
        raise HTTPException(status_code=500, detail=f"Verification failed: {str(e)}")

    finally:
        # Cleanup temporary files
        cleanup_temp_file(img1_path)
        cleanup_temp_file(img2_path)


@app.post("/face/embedding", response_model=FaceEmbeddingResponse)
async def extract_embedding(request: FaceEmbeddingRequest):
    """
    Extract face embedding from image.
    Useful for storing embeddings instead of images.
    """
    img_path = None

    try:
        img_path = base64_to_image(request.img_base64)
        model = request.model_name or settings.model_name

        # Extract embedding
        embedding_objs = DeepFace.represent(
            img_path=img_path,
            model_name=model,
            detector_backend=settings.detector_backend,
            enforce_detection=settings.enforce_detection
        )

        if not embedding_objs:
            raise HTTPException(status_code=400, detail="No face detected in image")

        embedding = embedding_objs[0]["embedding"]

        logger.info(f"Embedding extracted: model={model}, dimensions={len(embedding)}")

        return FaceEmbeddingResponse(
            embedding=embedding,
            model=model,
            dimensions=len(embedding)
        )

    except HTTPException:
        raise

    except Exception as e:
        logger.error(f"Error extracting embedding: {e}")
        raise HTTPException(status_code=500, detail=f"Embedding extraction failed: {str(e)}")

    finally:
        cleanup_temp_file(img_path)


# ============================================================================
# RUN
# ============================================================================

if __name__ == "__main__":
    import uvicorn

    uvicorn.run(
        "main:app",
        host="0.0.0.0",
        port=8080,
        reload=True,
        log_level="info"
    )
