package com.iykyk.facestory.data.ml

import android.graphics.Rect
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.iykyk.facestory.data.video.ExtractedFrame
import com.iykyk.facestory.domain.model.DetectedFace
import com.iykyk.facestory.util.UniversalLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class MLKitFaceDetector {

    companion object {
        private const val TAG = "MLKitFaceDetector"
    }

    private val options = FaceDetectorOptions.Builder()
        .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
        .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_NONE)
        .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
        .setMinFaceSize(0.015f)
        .build()

    private val detector = FaceDetection.getClient(options)

    suspend fun detectFaces(frame: ExtractedFrame): List<DetectedFace> = withContext(Dispatchers.Default) {
        val image = InputImage.fromBitmap(frame.bitmap, 0)

        suspendCancellableCoroutine { continuation ->
            detector.process(image)
                .addOnSuccessListener { faces ->
                    val detectedFaces = faces.map { face ->
                        face.toDomain(frame)
                    }
                    continuation.resume(detectedFaces)
                }
                .addOnFailureListener { exception ->
                    UniversalLogger.e(TAG, "Face detection failed for frame at ${frame.timestampMs}ms: ${exception.message}", exception)
                    continuation.resumeWithException(exception)
                }
        }
    }

    private fun Face.toDomain(frame: ExtractedFrame): DetectedFace {
        val clampedBox = Rect(
            boundingBox.left.coerceIn(0, frame.bitmap.width - 1),
            boundingBox.top.coerceIn(0, frame.bitmap.height - 1),
            boundingBox.right.coerceIn(1, frame.bitmap.width),
            boundingBox.bottom.coerceIn(1, frame.bitmap.height)
        )

        return DetectedFace(
            timestampMs = frame.timestampMs,
            boundingBox = clampedBox,
            frameWidth = frame.bitmap.width,
            frameHeight = frame.bitmap.height,
            headEulerX = headEulerAngleX,
            headEulerY = headEulerAngleY,
            headEulerZ = headEulerAngleZ,
            leftEyeOpenProbability = leftEyeOpenProbability,
            rightEyeOpenProbability = rightEyeOpenProbability,
            smilingProbability = smilingProbability,
            frame = frame.bitmap
        )
    }

    fun close() {
        detector.close()
    }
}
