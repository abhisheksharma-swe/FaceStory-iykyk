package com.iykyk.facestory.domain.usecase

import android.graphics.Bitmap
import android.net.Uri
import com.iykyk.facestory.data.ml.MLKitFaceDetector
import com.iykyk.facestory.data.ml.TFLiteFaceEmbedder
import com.iykyk.facestory.data.video.VideoFrameExtractor
import com.iykyk.facestory.domain.model.DetectedFace
import com.iykyk.facestory.domain.model.PersonCluster
import com.iykyk.facestory.ml.ImageQualityEvaluator
import com.iykyk.facestory.util.BitmapUtils
import com.iykyk.facestory.util.Constants
import com.iykyk.facestory.util.MathUtils
import com.iykyk.facestory.util.UniversalLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class NoFacesDetectedException(
    val framesScanned: Int,
    val blurrySkipped: Int
) : Exception("No faces detected in $framesScanned frames ($blurrySkipped blurry skipped)")

data class ProcessingResult(
    val people: List<PersonCluster>,
    val collage: Bitmap,
    val diagnostics: ProcessingDiagnostics
)

data class ProcessingDiagnostics(
    val framesScanned: Int,
    val faceDetections: Int,
    val faceTimestamps: Int,
    val appearanceSegments: Int,
    val embeddedSegments: Int,
    val identityClusters: Int,
    val totalAppearances: Int,
    val appearancesPerCluster: List<Int>
)

class ProcessVideoUseCase(
    private val frameExtractor: VideoFrameExtractor,
    private val faceDetector: MLKitFaceDetector,
    private val embedder: TFLiteFaceEmbedder,
    private val appearanceTracker: TrackAppearancesUseCase = TrackAppearancesUseCase(),
    private val representativeSelector: SelectRepresentativeUseCase = SelectRepresentativeUseCase(),
    private val clusterer: ClusterFacesUseCase = ClusterFacesUseCase(),
    private val collageGenerator: GenerateCollageUseCase = GenerateCollageUseCase()
) {
    companion object {
        private const val TAG = "ProcessVideoUseCase"
    }

    suspend operator fun invoke(
        videoUri: Uri,
        onProgress: (progress: Float, stage: String) -> Unit
    ): ProcessingResult = withContext(Dispatchers.Default) {
        val totalStartTime = System.currentTimeMillis()
        UniversalLogger.i(TAG, "Starting video processing pipeline for URI: $videoUri")

        try {
            onProgress(0.05f, "Extracting frames...")
            val extractedFrames = frameExtractor.extractFrames(videoUri) { frameProgress, currentMs, totalMs ->
                onProgress(0.05f + (frameProgress * 0.15f), "Extracting frames ($currentMs/$totalMs ms)...")
            }

            if (extractedFrames.isEmpty()) {
                val errorMsg = "Failed to extract frames from video ($videoUri)."
                UniversalLogger.e(TAG, errorMsg)
                throw IllegalStateException(errorMsg)
            }

            onProgress(0.20f, "Detecting faces...")
            val allDetectedFaces = mutableListOf<DetectedFace>()
            val totalFrames = extractedFrames.size.toFloat()
            var blurryFramesObserved = 0

            extractedFrames.forEachIndexed { index, frame ->
                if (frame.frameSharpness < Constants.MIN_FRAME_SHARPNESS) {
                    blurryFramesObserved++
                }

                val detectedInFrame = faceDetector.detectFaces(frame)
                detectedInFrame.forEach { face ->
                    face.faceSharpness = ImageQualityEvaluator.computeLaplacianVariance(face.frame, face.boundingBox)
                }

                allDetectedFaces.addAll(detectedInFrame)
                val progress = 0.20f + ((index / totalFrames) * 0.20f)
                onProgress(progress, "Detecting faces (${allDetectedFaces.size} found)...")
            }

            if (allDetectedFaces.isEmpty()) {
                UniversalLogger.w(TAG, "No faces detected across ${extractedFrames.size} frames")
                throw NoFacesDetectedException(
                    framesScanned = extractedFrames.size,
                    blurrySkipped = blurryFramesObserved
                )
            }

            onProgress(0.45f, "Tracking appearance segments...")
            val appearanceSegments = appearanceTracker(allDetectedFaces)

            if (appearanceSegments.isEmpty()) {
                val errorMsg = "No continuous appearance segments could be created from the detected faces."
                UniversalLogger.e(TAG, errorMsg)
                throw IllegalStateException(errorMsg)
            }

            onProgress(0.55f, "Scoring candidate frames...")
            val scoredSegments = representativeSelector.scoreSegments(appearanceSegments)

            onProgress(0.65f, "Generating face identities...")
            val totalSegments = scoredSegments.size.toFloat()

            scoredSegments.forEachIndexed { index, segment ->
                val candidates = segment.candidateFaces
                    .filter { it.faceSharpness >= Constants.MIN_FACE_CROP_SHARPNESS }
                    .ifEmpty { segment.candidateFaces }
                    .sortedByDescending { it.qualityScore }
                    .take(5)
                    .ifEmpty { listOfNotNull(segment.bestFrame).ifEmpty { segment.candidateFaces.take(1) } }

                val embeddings = candidates.mapNotNull { face ->
                    generateFaceEmbedding(face)
                }

                segment.embeddingSamples = embeddings
                segment.embedding = when {
                    embeddings.isEmpty() -> null
                    embeddings.size == 1 -> embeddings[0]
                    else -> MathUtils.calculateClusterCentroid(embeddings)
                }

                val progress = 0.65f + (((index + 1) / totalSegments) * 0.15f)
                onProgress(progress, "Generating face identities (${index + 1}/${scoredSegments.size})...")
            }

            onProgress(0.82f, "Grouping identities...")
            val rawClusters = clusterer(scoredSegments)
            val finalPeople = representativeSelector.selectClusterRepresentatives(rawClusters)

            val uniquePersonCount = finalPeople.size
            val collageTileCount = finalPeople.count { it.representativeFace != null }
            val totalAppearanceCount = scoredSegments.size
            val sumAppearanceCount = finalPeople.sumOf { it.appearanceCount }
            val diagnostics = ProcessingDiagnostics(
                framesScanned = extractedFrames.size,
                faceDetections = allDetectedFaces.size,
                faceTimestamps = allDetectedFaces.map { it.timestampMs }.distinct().size,
                appearanceSegments = scoredSegments.size,
                embeddedSegments = scoredSegments.count { it.embedding != null },
                identityClusters = finalPeople.size,
                totalAppearances = totalAppearanceCount,
                appearancesPerCluster = finalPeople.map { it.appearanceCount }
            )

            check(collageTileCount == uniquePersonCount) {
                "Invariant violation: collageTileCount ($collageTileCount) != uniquePersonCount ($uniquePersonCount)"
            }
            check(sumAppearanceCount == totalAppearanceCount) {
                "Invariant violation: sum(appearanceCount) ($sumAppearanceCount) != totalAppearanceCount ($totalAppearanceCount)"
            }

            onProgress(0.90f, "Rendering collage...")
            val collageBitmap = collageGenerator(finalPeople)

            val totalDuration = System.currentTimeMillis() - totalStartTime
            UniversalLogger.i(TAG, "Video processing pipeline completed in ${totalDuration}ms: $uniquePersonCount people, $sumAppearanceCount appearances")
            onProgress(1.0f, "Complete!")

            ProcessingResult(
                people = finalPeople,
                collage = collageBitmap,
                diagnostics = diagnostics
            )
        } catch (e: Exception) {
            val totalDuration = System.currentTimeMillis() - totalStartTime
            UniversalLogger.e(TAG, "Video processing pipeline failed after ${totalDuration}ms: ${e.message}", e)
            throw e
        }
    }

    private suspend fun generateFaceEmbedding(face: DetectedFace): FloatArray? {
        face.embedding?.let { return it }
        return try {
            val crop = BitmapUtils.getAlignedSquareFaceCropForEmbedding(
                frame = face.frame,
                boundingBox = face.boundingBox,
                headEulerZ = face.headEulerZ,
                targetSize = 112
            )
            embedder.generateEmbedding(crop).also { face.embedding = it }
        } catch (e: Exception) {
            UniversalLogger.w(TAG, "Embedding failed for face at ${face.timestampMs}ms: ${e.message}")
            null
        }
    }
}
