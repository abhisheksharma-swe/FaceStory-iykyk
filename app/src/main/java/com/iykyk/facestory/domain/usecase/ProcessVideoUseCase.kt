package com.iykyk.facestory.domain.usecase

import android.net.Uri
import com.iykyk.facestory.data.ml.MLKitFaceDetector
import com.iykyk.facestory.data.ml.TFLiteFaceEmbedder
import com.iykyk.facestory.data.video.VideoFrameExtractor
import android.graphics.Bitmap
import com.iykyk.facestory.domain.model.DetectedFace
import com.iykyk.facestory.domain.model.PersonCluster
import com.iykyk.facestory.util.BitmapUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class ProcessingResult(
    val people: List<PersonCluster>,
    val collage: Bitmap
)

class ProcessVideoUseCase(
    private val frameExtractor: VideoFrameExtractor,
    private val faceDetector: MLKitFaceDetector,
    private val embedder: TFLiteFaceEmbedder,
    private val clusterer: ClusterFacesUseCase = ClusterFacesUseCase(),
    private val appearanceTracker: TrackAppearancesUseCase = TrackAppearancesUseCase(),
    private val representativeSelector: SelectRepresentativeUseCase = SelectRepresentativeUseCase(),
    private val collageGenerator: GenerateCollageUseCase = GenerateCollageUseCase()
) {

    suspend operator fun invoke(
        videoUri: Uri,
        onProgress: (progress: Float, stage: String) -> Unit
    ): ProcessingResult = withContext(Dispatchers.Default) {

        onProgress(0.05f, "Extracting frames (3 FPS)...")
        val extractedFrames = frameExtractor.extractFrames(videoUri) { frameProgress, _, _ ->
            onProgress(0.05f + (frameProgress * 0.20f), "Extracting frames...")
        }

        if (extractedFrames.isEmpty()) {
            throw IllegalStateException("Failed to extract frames from video")
        }

        onProgress(0.25f, "Detecting faces...")
        val allDetectedFaces = mutableListOf<DetectedFace>()
        val totalFrames = extractedFrames.size.toFloat()

        extractedFrames.forEachIndexed { index, frame ->
            val faces = faceDetector.detectFaces(frame)
            allDetectedFaces.addAll(faces)
            val progress = 0.25f + ((index / totalFrames) * 0.25f)
            onProgress(progress, "Detecting faces (${allDetectedFaces.size} found)...")
        }

        if (allDetectedFaces.isEmpty()) {
            throw IllegalStateException("No faces detected in video")
        }

        onProgress(0.50f, "Generating face embeddings...")
        val totalFaces = allDetectedFaces.size.toFloat()

        allDetectedFaces.forEachIndexed { index, face ->
            val faceCrop = BitmapUtils.getExpandedFaceCrop(face.frame, face.boundingBox)
            face.embedding = embedder.generateEmbedding(faceCrop)
            val progress = 0.50f + ((index / totalFaces) * 0.20f)
            onProgress(progress, "Generating face embeddings...")
        }

        onProgress(0.70f, "Grouping identities...")
        val initialClusters = clusterer(allDetectedFaces)

        onProgress(0.80f, "Counting appearances...")
        val trackedClusters = appearanceTracker(initialClusters)

        onProgress(0.85f, "Selecting best portraits...")
        val finalPeople = representativeSelector(trackedClusters)

        onProgress(0.92f, "Rendering collage...")
        val collageBitmap = collageGenerator(finalPeople)

        onProgress(1.0f, "Complete!")
        ProcessingResult(
            people = finalPeople,
            collage = collageBitmap
        )
    }
}
