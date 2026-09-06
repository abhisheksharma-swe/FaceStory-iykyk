package com.iykyk.facestory.domain.model

import android.graphics.Bitmap
import android.graphics.Rect

data class DetectedFace(
    val timestampMs: Long,
    val boundingBox: Rect,
    val frameWidth: Int,
    val frameHeight: Int,

    val headEulerX: Float?,
    val headEulerY: Float?,
    val headEulerZ: Float?,

    val leftEyeOpenProbability: Float?,
    val rightEyeOpenProbability: Float?,
    val smilingProbability: Float?,

    var embedding: FloatArray? = null,
    var qualityScore: Float = 0f,
    var faceSharpness: Float = -1f,
    val frame: Bitmap
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as DetectedFace
        return timestampMs == other.timestampMs && boundingBox == other.boundingBox
    }

    override fun hashCode(): Int {
        var result = timestampMs.hashCode()
        result = 31 * result + boundingBox.hashCode()
        return result
    }
}