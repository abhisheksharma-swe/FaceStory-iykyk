package com.iykyk.facestory.domain.usecase


import com.iykyk.facestory.domain.model.DetectedFace
import com.iykyk.facestory.domain.model.PersonCluster
import com.iykyk.facestory.util.Constants
import com.iykyk.facestory.util.MathUtils

class ClusterFacesUseCase(
    private val similarityThreshold: Float = Constants.IDENTITY_SIMILARITY_THRESHOLD
) {
    operator fun invoke(allFaces: List<DetectedFace>): List<PersonCluster> {
        val clusters = mutableListOf<PersonCluster>()
        var nextPersonId = 1

        val framesByTimestamp = allFaces.groupBy { it.timestampMs }.toSortedMap()

        for ((_, facesInFrame) in framesByTimestamp) {
            val clustersUsedInThisFrame = mutableSetOf<Int>()

            for (face in facesInFrame) {
                val embedding = face.embedding ?: continue

                var bestMatchCluster: PersonCluster? = null
                var bestSimilarity = -1f

                for (cluster in clusters) {
                    if (clustersUsedInThisFrame.contains(cluster.id)) continue

                    val clusterCentroid = cluster.representativeEmbedding ?: continue
                    val similarity = MathUtils.cosineSimilarity(embedding, clusterCentroid)

                    if (similarity > bestSimilarity) {
                        bestSimilarity = similarity
                        bestMatchCluster = cluster
                    }
                }

                if (bestMatchCluster != null && bestSimilarity >= similarityThreshold) {
                    bestMatchCluster.faces.add(face)
                    clustersUsedInThisFrame.add(bestMatchCluster.id)

                    val embeddings = bestMatchCluster.faces.mapNotNull { it.embedding }
                    bestMatchCluster.representativeEmbedding = MathUtils.calculateClusterCentroid(embeddings)
                } else {
                    val newCluster = PersonCluster(
                        id = nextPersonId++,
                        faces = mutableListOf(face),
                        representativeEmbedding = embedding
                    )
                    clusters.add(newCluster)
                    clustersUsedInThisFrame.add(newCluster.id)
                }
            }
        }

        return clusters
    }
}
