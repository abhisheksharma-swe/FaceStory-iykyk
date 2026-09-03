package com.iykyk.facestory.domain.model

class AppearanceSegment (
    val personId: Int,
    val startTimeMs:Long,
    var endTimeMs: Long,
    val faces: MutableList<DetectedFace> = mutableListOf()
)


