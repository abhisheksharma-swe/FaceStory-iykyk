package com.iykyk.facestory.domain.model

data class PersonCluster(
    val id : Int,
    val faces: MutableList<DetectedFace> = mutableListOf(),
    val appearances: MutableList<AppearanceSegment> = mutableListOf(),
    var representativeEmbedding: FloatArray? = null

){
    val appearanceCount: Int
        get() = appearances.size


    var representativeFace: DetectedFace? = null
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as PersonCluster

        if (id != other.id) return false
        if (faces != other.faces) return false
        if (appearances != other.appearances) return false
        if (!representativeEmbedding.contentEquals(other.representativeEmbedding)) return false
        if (representativeFace != other.representativeFace) return false
        if (appearanceCount != other.appearanceCount) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id
        result = 31 * result + faces.hashCode()
        result = 31 * result + appearances.hashCode()
        result = 31 * result + (representativeEmbedding?.contentHashCode() ?: 0)
        result = 31 * result + (representativeFace?.hashCode() ?: 0)
        result = 31 * result + appearanceCount
        return result
    }
}