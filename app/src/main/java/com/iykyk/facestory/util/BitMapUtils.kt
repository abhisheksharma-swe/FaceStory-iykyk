package com.iykyk.facestory.util

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import kotlin.math.abs
import kotlin.math.max

object BitmapUtils {

    fun getAlignedSquareFaceCropForEmbedding(
        frame: Bitmap,
        boundingBox: Rect,
        headEulerZ: Float? = null,
        targetSize: Int = 112
    ): Bitmap {
        val faceW = boundingBox.width().toFloat()
        val faceH = boundingBox.height().toFloat()
        val cx = boundingBox.centerX().toFloat()
        val cy = boundingBox.centerY().toFloat()

        val side = (max(faceW, faceH) * 1.15f)
            .coerceAtMost(minOf(frame.width, frame.height).toFloat())
            .coerceAtLeast(1f)
        val srcLeft = (cx - side / 2f).coerceIn(0f, frame.width - side)
        val srcTop = (cy - side / 2f).coerceIn(0f, frame.height - side)
        val srcRight = srcLeft + side
        val srcBottom = srcTop + side

        val srcRect = RectF(srcLeft, srcTop, srcRight, srcBottom)
        val dstRect = RectF(0f, 0f, targetSize.toFloat(), targetSize.toFloat())

        val output = Bitmap.createBitmap(targetSize, targetSize, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)

        val matrix = Matrix()
        matrix.setRectToRect(srcRect, dstRect, Matrix.ScaleToFit.FILL)

        val roll = headEulerZ ?: 0f
        if (abs(roll) > 3f) {
            matrix.postRotate(-roll, targetSize / 2f, targetSize / 2f)
        }

        val paint = Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG)
        canvas.drawBitmap(frame, matrix, paint)
        return output
    }

    fun getExpandedFaceCrop(
        frame: Bitmap,
        boundingBox: Rect,
        expansionFactor: Float = 1.8f
    ): Bitmap {
        val width = boundingBox.width()
        val height = boundingBox.height()

        val centerX = boundingBox.centerX()
        val centerY = boundingBox.centerY()

        val expandedWidth = (width * expansionFactor).toInt()
        val expandedHeight = (height * expansionFactor).toInt()

        val left = (centerX - expandedWidth / 2).coerceIn(0, frame.width - 1)
        val top = (centerY - expandedHeight / 2).coerceIn(0, frame.height - 1)
        val right = (centerX + expandedWidth / 2).coerceIn(left + 1, frame.width)
        val bottom = (centerY + expandedHeight / 2).coerceIn(top + 1, frame.height)

        val cropWidth = (right - left).coerceAtLeast(1)
        val cropHeight = (bottom - top).coerceAtLeast(1)

        return Bitmap.createBitmap(frame, left, top, cropWidth, cropHeight)
    }

    fun getPortraitCropForCollage(frame: Bitmap, boundingBox: Rect, targetW: Int, targetH: Int): Bitmap {
        val expansion = 2.2f
        val faceW = boundingBox.width()
        val faceH = boundingBox.height()
        val cx = boundingBox.centerX()
        val cy = boundingBox.centerY()

        val expW = (faceW * expansion).toInt()
        val expH = (faceH * expansion).toInt()

        val srcLeft = (cx - expW / 2).coerceIn(0, frame.width - 1)
        val srcTop = (cy - expH / 2).coerceIn(0, frame.height - 1)
        val srcRight = (cx + expW / 2).coerceIn(srcLeft + 1, frame.width)
        val srcBottom = (cy + expH / 2).coerceIn(srcTop + 1, frame.height)

        val srcRect = Rect(srcLeft, srcTop, srcRight, srcBottom)
        val dstRect = Rect(0, 0, targetW.coerceAtLeast(1), targetH.coerceAtLeast(1))

        val output = Bitmap.createBitmap(dstRect.width(), dstRect.height(), Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)

        val paint = Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG)
        canvas.drawBitmap(frame, srcRect, dstRect, paint)
        return output
    }

    fun toEmbeddingInput(faceCrop: Bitmap, targetSize: Int = 112): Bitmap {
        return Bitmap.createScaledBitmap(faceCrop, targetSize, targetSize, true)
    }
}
