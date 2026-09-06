package com.iykyk.facestory.domain.usecase

import com.iykyk.facestory.domain.model.AppearanceSegment
import com.iykyk.facestory.domain.model.PersonCluster
import com.iykyk.facestory.ml.ImageQualityEvaluator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SelectRepresentativeUseCase {

    suspend fun scoreSegments(segments: List<AppearanceSegment>): List<AppearanceSegment> = withContext(Dispatchers.Default) {
        for (segment in segments) {
            val candidates = segment.candidateFaces
            if (candidates.isEmpty()) continue

            val rawSharpnessList = candidates.map { face ->
                ImageQualityEvaluator.computeLaplacianVariance(face.frame, face.boundingBox)
            }

            val minSharpness = rawSharpnessList.minOrNull() ?: 0f
            val maxSharpness = rawSharpnessList.maxOrNull() ?: 1f
            val sharpnessRange = (maxSharpness - minSharpness).coerceAtLeast(1e-5f)

            candidates.forEachIndexed { index, face ->
                val normalizedSharpness = ((rawSharpnessList[index] - minSharpness) / sharpnessRange).coerceIn(0f, 1f)
                face.qualityScore = ImageQualityEvaluator.calculateScore(face, normalizedSharpness, rawSharpnessList[index])
            }

            val bestFace = candidates.maxByOrNull { it.qualityScore } ?: candidates.first()
            segment.bestFrame = bestFace
        }
        segments
    }

    suspend fun selectClusterRepresentatives(clusters: List<PersonCluster>): List<PersonCluster> = withContext(Dispatchers.Default) {
        for (cluster in clusters) {
            val bestSegmentFrames = cluster.appearances.mapNotNull { it.bestFrame }
            val fallbackCandidates = cluster.faces

            val chosenFace = bestSegmentFrames.maxByOrNull { it.qualityScore }
                ?: fallbackCandidates.maxByOrNull { it.qualityScore }
                ?: fallbackCandidates.firstOrNull()

            cluster.representativeFace = chosenFace
        }
        clusters
    }
}
