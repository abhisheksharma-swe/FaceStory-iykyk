package com.iykyk.facestory.domain.model

data class PersonCluster(
    val id: Int,
    val appearances: MutableList<AppearanceSegment> = mutableListOf(),
    var representativeEmbedding: FloatArray? = null,
    var representativeFace: DetectedFace? = null
) {
    val appearanceCount: Int
        get() = appearances.size

    val faces: List<DetectedFace>
        get() = appearances.flatMap { it.candidateFaces }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as PersonCluster
        return id == other.id &&
                appearances == other.appearances &&
                representativeFace == other.representativeFace
    }

    override fun hashCode(): Int {
        var result = id
        result = 31 * result + appearances.hashCode()
        result = 31 * result + (representativeFace?.hashCode() ?: 0)
        return result
    }
}
