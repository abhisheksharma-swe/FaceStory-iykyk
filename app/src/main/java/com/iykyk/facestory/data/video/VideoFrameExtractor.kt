package com.iykyk.facestory.data.video

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.media.MediaMetadataRetriever
import android.net.Uri
import com.iykyk.facestory.util.UniversalLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

data class ExtractedFrame(
    val timestampMs: Long,
    val bitmap: Bitmap,
    val frameSharpness: Float = 0f
)

class VideoFrameExtractor(
    private val context: Context,
    private val fps: Int = 8,
    private val maxDimension: Int = 1080
) {
    companion object {
        private const val TAG = "VideoFrameExtractor"
        private const val HIGH_MOTION_LUMA_DELTA = 18f
    }

    suspend fun extractFrames(
        videoUri: Uri,
        onProgress: (progress: Float, currentMs: Long, totalMs: Long) -> Unit = { _, _, _ -> }
    ): List<ExtractedFrame> = withContext(Dispatchers.IO) {
        val retriever = MediaMetadataRetriever()
        val frames = mutableListOf<ExtractedFrame>()
        var tempFile: File? = null

        try {
            if (videoUri.scheme == "content") {
                val file = File(context.cacheDir, "temp_video_${System.currentTimeMillis()}.mp4")
                context.contentResolver.openInputStream(videoUri)?.use { input ->
                    FileOutputStream(file).use { output ->
                        input.copyTo(output)
                    }
                }
                tempFile = file
                retriever.setDataSource(file.absolutePath)
            } else if (videoUri.scheme == "file" && videoUri.path != null) {
                retriever.setDataSource(videoUri.path)
            } else {
                retriever.setDataSource(context, videoUri)
            }

            val durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            val durationMs = durationStr?.toLongOrNull() ?: 0L

            val rotationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)
            val rotationDegrees = rotationStr?.toIntOrNull() ?: 0

            if (durationMs <= 0L) {
                UniversalLogger.w(TAG, "Invalid or zero video duration detected ($durationMs ms)")
                return@withContext emptyList()
            }

            val intervalMs = 1000L / fps
            var currentMs = 0L
            var previousPrimaryFrame: Bitmap? = null

            while (currentMs < durationMs) {
                val primaryFrame = extractFrameAt(retriever, currentMs, rotationDegrees)

                if (primaryFrame != null) {
                    frames.add(primaryFrame)
                    val highMotion = previousPrimaryFrame?.let {
                        calculateMotion(it, primaryFrame.bitmap) >= HIGH_MOTION_LUMA_DELTA
                    } ?: false

                    if (highMotion && currentMs + (intervalMs / 2) < durationMs) {
                        extractFrameAt(retriever, currentMs + (intervalMs / 2), rotationDegrees)?.let { recoveryFrame ->
                            frames.add(recoveryFrame)
                        }
                    }
                    previousPrimaryFrame = primaryFrame.bitmap
                }

                val progress = (currentMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
                onProgress(progress, currentMs, durationMs)

                currentMs += intervalMs
            }
        } catch (ex: Exception) {
            UniversalLogger.e(TAG, "Error during frame extraction: ${ex.message}", ex)
        } finally {
            try {
                retriever.release()
            } catch (e: Exception) {
                // ignore
            }
            try {
                tempFile?.delete()
            } catch (e: Exception) {
                // ignore
            }
        }
        frames
    }

    private fun extractFrameAt(
        retriever: MediaMetadataRetriever,
        timestampMs: Long,
        rotationDegrees: Int
    ): ExtractedFrame? {
        val rawBitmap = retriever.getFrameAtTime(
            timestampMs * 1000L,
            MediaMetadataRetriever.OPTION_CLOSEST
        ) ?: return null
        val processedBitmap = processFrame(rawBitmap, maxDimension, rotationDegrees)
        return ExtractedFrame(
            timestampMs = timestampMs,
            bitmap = processedBitmap,
            frameSharpness = computeFrameSharpness(processedBitmap)
        )
    }

    private fun calculateMotion(previous: Bitmap, current: Bitmap): Float {
        val sampleSize = 64
        val previousSample = Bitmap.createScaledBitmap(previous, sampleSize, sampleSize, true)
        val currentSample = Bitmap.createScaledBitmap(current, sampleSize, sampleSize, true)
        var difference = 0.0
        for (y in 0 until sampleSize step 2) {
            for (x in 0 until sampleSize step 2) {
                val oldColor = previousSample.getPixel(x, y)
                val newColor = currentSample.getPixel(x, y)
                val oldLuma = 0.299f * android.graphics.Color.red(oldColor) + 0.587f * android.graphics.Color.green(oldColor) + 0.114f * android.graphics.Color.blue(oldColor)
                val newLuma = 0.299f * android.graphics.Color.red(newColor) + 0.587f * android.graphics.Color.green(newColor) + 0.114f * android.graphics.Color.blue(newColor)
                difference += kotlin.math.abs(newLuma - oldLuma)
            }
        }
        return (difference / ((sampleSize / 2) * (sampleSize / 2))).toFloat()
    }

    private fun processFrame(bitmap: Bitmap, maxDim: Int, rotationDegrees: Int): Bitmap {
        var workingBitmap = bitmap
        val needsRotation = when (rotationDegrees) {
            90, 270 -> bitmap.width > bitmap.height
            180 -> true
            else -> false
        }

        if (needsRotation) {
            val matrix = Matrix().apply { postRotate(rotationDegrees.toFloat()) }
            workingBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        }

        val width = workingBitmap.width
        val height = workingBitmap.height
        if (width <= maxDim && height <= maxDim) return workingBitmap

        val scale = maxDim.toFloat() / maxOf(width, height).toFloat()
        val newWidth = (width * scale).toInt().coerceAtLeast(1)
        val newHeight = (height * scale).toInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(workingBitmap, newWidth, newHeight, true)
    }

    private fun computeFrameSharpness(bitmap: Bitmap): Float {
        val sampleSize = 240
        val scale = sampleSize.toFloat() / maxOf(bitmap.width, bitmap.height).toFloat()
        val sw = (bitmap.width * scale).toInt().coerceAtLeast(4)
        val sh = (bitmap.height * scale).toInt().coerceAtLeast(4)
        val small = Bitmap.createScaledBitmap(bitmap, sw, sh, true)

        var sum = 0.0
        var sumSq = 0.0
        var count = 0

        for (y in 1 until sh - 1) {
            for (x in 1 until sw - 1) {
                val c = small.getPixel(x, y)
                val gray = 0.299f * android.graphics.Color.red(c) +
                           0.587f * android.graphics.Color.green(c) +
                           0.114f * android.graphics.Color.blue(c)
                val t = run { val p = small.getPixel(x, y - 1); 0.299f * android.graphics.Color.red(p) + 0.587f * android.graphics.Color.green(p) + 0.114f * android.graphics.Color.blue(p) }
                val b = run { val p = small.getPixel(x, y + 1); 0.299f * android.graphics.Color.red(p) + 0.587f * android.graphics.Color.green(p) + 0.114f * android.graphics.Color.blue(p) }
                val l = run { val p = small.getPixel(x - 1, y); 0.299f * android.graphics.Color.red(p) + 0.587f * android.graphics.Color.green(p) + 0.114f * android.graphics.Color.blue(p) }
                val r = run { val p = small.getPixel(x + 1, y); 0.299f * android.graphics.Color.red(p) + 0.587f * android.graphics.Color.green(p) + 0.114f * android.graphics.Color.blue(p) }
                val laplacian = t + b + l + r - 4f * gray
                sum += laplacian
                sumSq += laplacian * laplacian
                count++
            }
        }
        if (count == 0) return 0f
        val mean = sum / count
        return ((sumSq / count) - (mean * mean)).toFloat().coerceAtLeast(0f)
    }
}
