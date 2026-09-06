package com.iykyk.facestory.domain.usecase

import com.iykyk.facestory.domain.model.AppearanceSegment
import com.iykyk.facestory.domain.model.PersonCluster
import com.iykyk.facestory.util.Constants
import com.iykyk.facestory.util.MathUtils
import kotlin.math.max
import kotlin.math.min

class ClusterFacesUseCase(
    private val similarityThreshold: Float = Constants.IDENTITY_SIMILARITY_THRESHOLD
) {
    operator fun invoke(segments: List<AppearanceSegment>): List<PersonCluster> {
        if (segments.isEmpty()) return emptyList()

        val clusters = segments.mapIndexed { index, segment ->
            PersonCluster(
                id = index + 1,
                appearances = mutableListOf(segment),
                representativeEmbedding = segment.embedding?.clone()
            )
        }.toMutableList()

        while (clusters.size > 1) {
            var bestPairI = -1
            var bestPairJ = -1
            var bestSimilarity = -1f

            for (i in 0 until clusters.size) {
                for (j in i + 1 until clusters.size) {
                    val clusterA = clusters[i]
                    val clusterB = clusters[j]

                    val hasCoOccurrence = clusterA.appearances.any { segA ->
                        clusterB.appearances.any { segB -> hasOverlap(segA, segB) }
                    }
                    if (hasCoOccurrence) continue

                    val centroidA = clusterA.representativeEmbedding ?: continue
                    val centroidB = clusterB.representativeEmbedding ?: continue
                    val centroidSim = MathUtils.cosineSimilarity(centroidA, centroidB)
                    val robustSampleSim = robustSampleSimilarityBetweenClusters(clusterA, clusterB)
                    val sim = if (robustSampleSim > 0f) {
                        (0.50f * centroidSim) + (0.50f * robustSampleSim)
                    } else centroidSim

                    if (sim > bestSimilarity) {
                        bestSimilarity = sim
                        bestPairI = i
                        bestPairJ = j
                    }
                }
            }

            if (bestPairI == -1 || bestSimilarity < similarityThreshold) {
                break
            }

            val clusterA = clusters[bestPairI]
            val clusterB = clusters[bestPairJ]

            clusterA.appearances.addAll(clusterB.appearances)
            val allEmb = clusterA.appearances.mapNotNull { it.embedding }
            clusterA.representativeEmbedding = MathUtils.calculateClusterCentroid(allEmb)
            clusters.removeAt(bestPairJ)
        }

        clusters.sortBy { it.appearances.minOf { seg -> seg.startTimeMs } }
        return clusters.mapIndexed { idx, cluster ->
            PersonCluster(
                id = idx + 1,
                appearances = cluster.appearances.sortedBy { it.startTimeMs }.toMutableList(),
                representativeEmbedding = cluster.representativeEmbedding,
                representativeFace = cluster.representativeFace
            )
        }
    }

    private fun robustSampleSimilarityBetweenClusters(clusterA: PersonCluster, clusterB: PersonCluster): Float {
        val scores = mutableListOf<Float>()
        for (segA in clusterA.appearances) {
            val samplesA = segA.embeddingSamples.ifEmpty { listOfNotNull(segA.embedding) }
            for (segB in clusterB.appearances) {
                val samplesB = segB.embeddingSamples.ifEmpty { listOfNotNull(segB.embedding) }
                for (embA in samplesA) {
                    for (embB in samplesB) {
                        scores += MathUtils.cosineSimilarity(embA, embB)
                    }
                }
            }
        }
        return scores.sortedDescending().take(3).let { top ->
            if (top.isEmpty()) -1f else top.average().toFloat()
        }
    }

    private fun hasOverlap(seg1: AppearanceSegment, seg2: AppearanceSegment): Boolean {
        val overlapStart = max(seg1.startTimeMs, seg2.startTimeMs)
        val overlapEnd = min(seg1.endTimeMs, seg2.endTimeMs)
        return overlapEnd >= overlapStart
    }
}
