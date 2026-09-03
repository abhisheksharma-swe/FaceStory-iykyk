package com.iykyk.facestory.util


import android.graphics.Bitmap
import android.graphics.Rect

object BitmapUtils {

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

        val cropWidth = right - left
        val cropHeight = bottom - top

        return Bitmap.createBitmap(frame, left, top, cropWidth, cropHeight)
    }


    fun toEmbeddingInput(faceCrop: Bitmap, targetSize: Int = 112): Bitmap {
        return Bitmap.createScaledBitmap(faceCrop, targetSize, targetSize, true)
    }
}
