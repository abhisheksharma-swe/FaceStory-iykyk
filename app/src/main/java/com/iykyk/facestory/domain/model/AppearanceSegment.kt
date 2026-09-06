package com.iykyk.facestory.domain.model

data class AppearanceSegment(
    val id: Int,
    var startTimeMs: Long,
    var endTimeMs: Long,
    val candidateFaces: MutableList<DetectedFace> = mutableListOf(),
    var bestFrame: DetectedFace? = null,
    var embedding: FloatArray? = null,
    var embeddingSamples: List<FloatArray> = emptyList()
) {
    val durationMs: Long
        get() = endTimeMs - startTimeMs

    val frameCount: Int
        get() = candidateFaces.size

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as AppearanceSegment
        return id == other.id && startTimeMs == other.startTimeMs && endTimeMs == other.endTimeMs
    }

    override fun hashCode(): Int {
        var result = id
        result = 31 * result + startTimeMs.hashCode()
        result = 31 * result + endTimeMs.hashCode()
        return result
    }
}
