package com.iykyk.facestory.util

import kotlin.math.sqrt

object MathUtils {


    fun cosineSimilarity(v1: FloatArray, v2: FloatArray): Float {
        require(v1.size == v2.size) { "Embedding dimensions must match: ${v1.size} vs ${v2.size}" }
        var dot = 0f
        for (i in v1.indices) {
            dot += v1[i] * v2[i]
        }
        return dot.coerceIn(-1f, 1f)
    }

    fun l2Normalize(vector: FloatArray): FloatArray {
        var sumSquare = 0f
        for (value in vector) {
            sumSquare += value * value
        }
        val norm = sqrt(sumSquare)
        if (norm == 0f) return vector

        val normalized = FloatArray(vector.size)
        for (i in vector.indices) {
            normalized[i] = vector[i] / norm
        }
        return normalized
    }


    fun calculateClusterCentroid(embeddings: List<FloatArray>): FloatArray {
        if (embeddings.isEmpty()) return FloatArray(0)
        val dimension = embeddings[0].size
        val centroid = FloatArray(dimension)

        for (emb in embeddings) {
            for (i in 0 until dimension) {
                centroid[i] += emb[i]
            }
        }

        val count = embeddings.size.toFloat()
        for (i in 0 until dimension) {
            centroid[i] /= count
        }

        return l2Normalize(centroid)
    }
}
