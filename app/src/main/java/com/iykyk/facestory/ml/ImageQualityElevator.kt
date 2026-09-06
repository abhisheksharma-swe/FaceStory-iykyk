package com.iykyk.facestory.ml

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Rect
import com.iykyk.facestory.domain.model.DetectedFace
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

object ImageQualityEvaluator {

    fun calculateScore(
        face: DetectedFace,
        normalizedSharpness: Float,
        rawSharpness: Float
    ): Float {
        val faceMinDim = minOf(face.boundingBox.width(), face.boundingBox.height())
        if (faceMinDim < 20) return 0.0f

        val sharpnessScore = when {
            rawSharpness < 18f -> (rawSharpness / 18f) * 0.15f
            rawSharpness < 45f -> 0.15f + ((rawSharpness - 18f) / 27f) * 0.35f
            else -> 0.50f + (normalizedSharpness * 0.50f)
        }.coerceIn(0f, 1f)

        val yaw = abs(face.headEulerY ?: 0f)
        val pitch = abs(face.headEulerX ?: 0f)
        val roll = abs(face.headEulerZ ?: 0f)
        val headDeviation = (yaw * 1.3f + pitch * 1.0f + roll * 0.8f)
        val frontality = (1f - (headDeviation / 32f)).coerceIn(0f, 1f)

        val leftEye = face.leftEyeOpenProbability ?: 0.80f
        val rightEye = face.rightEyeOpenProbability ?: 0.80f
        val minEye = min(leftEye, rightEye)
        val avgEye = (leftEye + rightEye) / 2f
        val eyesOpenScore = if (minEye < 0.40f) {
            0.10f * avgEye
        } else {
            avgEye.coerceIn(0f, 1f)
        }

        val faceW = face.boundingBox.width().toFloat()
        val faceH = face.boundingBox.height().toFloat()
        val faceDiag = sqrt(faceW * faceW + faceH * faceH)
        val frameDiag = sqrt((face.frameWidth * face.frameWidth + face.frameHeight * face.frameHeight).toFloat()).coerceAtLeast(1f)
        val diagRatio = faceDiag / frameDiag

        val scaleScore = when {
            diagRatio < 0.08f -> (diagRatio / 0.08f) * 0.30f
            diagRatio <= 0.35f -> 0.30f + ((diagRatio - 0.08f) / 0.27f) * 0.70f
            diagRatio <= 0.60f -> 1.0f
            else -> 0.80f
        }.coerceIn(0f, 1f)

        val smile = (face.smilingProbability ?: 0.35f).coerceIn(0f, 1f)

        val boundaryPenalty = calculateBoundaryPenalty(face.boundingBox, face.frameWidth, face.frameHeight)

        val compositeScore = (0.30f * sharpnessScore) +
                (0.25f * frontality) +
                (0.20f * eyesOpenScore) +
                (0.15f * scaleScore) +
                (0.10f * smile) -
                boundaryPenalty

        return compositeScore.coerceIn(0f, 1f)
    }

    fun computeLaplacianVariance(bitmap: Bitmap, box: Rect): Float {
        val left = box.left.coerceIn(0, bitmap.width - 1)
        val top = box.top.coerceIn(0, bitmap.height - 1)
        val right = box.right.coerceIn(left + 1, bitmap.width)
        val bottom = box.bottom.coerceIn(top + 1, bitmap.height)

        val cropWidth = right - left
        val cropHeight = bottom - top
        if (cropWidth < 12 || cropHeight < 12) return 0f

        val sampleSize = 180
        val scale = min(1f, sampleSize.toFloat() / max(cropWidth, cropHeight).toFloat())
        val sw = max(8, (cropWidth * scale).toInt())
        val sh = max(8, (cropHeight * scale).toInt())

        val crop = Bitmap.createBitmap(bitmap, left, top, cropWidth, cropHeight)
        val scaled = Bitmap.createScaledBitmap(crop, sw, sh, true)

        val gray = Array(sh) { FloatArray(sw) }
        for (y in 0 until sh) {
            for (x in 0 until sw) {
                val pixel = scaled.getPixel(x, y)
                gray[y][x] = 0.299f * Color.red(pixel) + 0.587f * Color.green(pixel) + 0.114f * Color.blue(pixel)
            }
        }

        var sum = 0.0
        var sumSq = 0.0
        var count = 0

        for (y in 1 until sh - 1) {
            for (x in 1 until sw - 1) {
                val laplacian = gray[y - 1][x] + gray[y + 1][x] + gray[y][x - 1] + gray[y][x + 1] - (4f * gray[y][x])
                sum += laplacian
                sumSq += laplacian * laplacian
                count++
            }
        }

        if (count == 0) return 0f
        val mean = sum / count
        val variance = (sumSq / count) - (mean * mean)
        return variance.toFloat().coerceAtLeast(0f)
    }

    private fun calculateBoundaryPenalty(box: Rect, frameW: Int, frameH: Int): Float {
        val marginX = frameW * 0.045f
        val marginY = frameH * 0.045f

        var penalty = 0f
        if (box.left < marginX) penalty += 0.25f
        if (box.top < marginY) penalty += 0.25f
        if (box.right > frameW - marginX) penalty += 0.25f
        if (box.bottom > frameH - marginY) penalty += 0.25f

        return penalty.coerceIn(0f, 0.65f)
    }
}
