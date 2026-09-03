package com.iykyk.facestory.ml


import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Rect
import com.iykyk.facestory.domain.model.DetectedFace
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

object ImageQualityEvaluator {

//     Scores a face from 0.0 to 1.0 based on pose, eyes, smile, size, and edge distance.
    //     [normalizedSharpness] is passed in after normalizing across all candidates for this person.

    fun calculateScore(
        face: DetectedFace,
        normalizedSharpness: Float
    ): Float {
        val yaw = abs(face.headEulerY ?: 0f)
        val roll = abs(face.headEulerZ ?: 0f)
        val frontality = (1f - min(1f, (yaw + roll) / 45f)).coerceIn(0f, 1f)

        val leftEye = face.leftEyeOpenProbability ?: 0.7f
        val rightEye = face.rightEyeOpenProbability ?: 0.7f
        val eyesOpen = ((leftEye + rightEye) / 2f).coerceIn(0f, 1f)

        val smile = (face.smilingProbability ?: 0.3f).coerceIn(0f, 1f)

        val faceArea = (face.boundingBox.width() * face.boundingBox.height()).toFloat()
        val frameArea = (face.frameWidth * face.frameHeight).toFloat().coerceAtLeast(1f)
        val areaRatio = (faceArea / frameArea * 10f).coerceIn(0f, 1f) // 10% of screen = 1.0

        val boundaryPenalty = calculateBoundaryPenalty(face.boundingBox, face.frameWidth, face.frameHeight)

        val rawScore = (0.30f * frontality) +
                (0.25f * normalizedSharpness) +
                (0.20f * eyesOpen) +
                (0.15f * smile) +
                (0.10f * areaRatio) -
                boundaryPenalty

        return rawScore.coerceIn(0f, 1f)
    }


    fun computeLaplacianVariance(bitmap: Bitmap, box: Rect): Float {
        val left = box.left.coerceIn(0, bitmap.width - 1)
        val top = box.top.coerceIn(0, bitmap.height - 1)
        val right = box.right.coerceIn(left + 1, bitmap.width)
        val bottom = box.bottom.coerceIn(top + 1, bitmap.height)

        val cropWidth = right - left
        val cropHeight = bottom - top
        if (cropWidth < 10 || cropHeight < 10) return 0f

        val sampleSize = 80
        val scale = min(1f, sampleSize.toFloat() / max(cropWidth, cropHeight).toFloat())
        val sw = max(5, (cropWidth * scale).toInt())
        val sh = max(5, (cropHeight * scale).toInt())

        val crop = Bitmap.createBitmap(bitmap, left, top, cropWidth, cropHeight)
        val scaled = Bitmap.createScaledBitmap(crop, sw, sh, false)

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
        val marginX = frameW * 0.03f
        val marginY = frameH * 0.03f

        var penalty = 0f
        if (box.left < marginX) penalty += 0.25f
        if (box.top < marginY) penalty += 0.25f
        if (box.right > frameW - marginX) penalty += 0.25f
        if (box.bottom > frameH - marginY) penalty += 0.25f

        return penalty.coerceIn(0f, 0.5f)
    }
}
