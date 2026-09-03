package com.iykyk.facestory.data.video

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class ExtractedFrame(
    val timestampMs: Long,
    val bitmap: Bitmap
)

class VideoFrameExtractor(
    private val context: Context,
    private val fps: Int = 3,
    private val maxDimension: Int = 1280
) {
    suspend fun extractFrames(
        videoUri: Uri,
        onProgress: (progress: Float, currentMs: Long, totalMs: Long) -> Unit = { _, _, _ -> }
    ): List<ExtractedFrame> = withContext(Dispatchers.IO) {

        val retriever = MediaMetadataRetriever()
        val frames = mutableListOf<ExtractedFrame>()

        try {
            retriever.setDataSource(context, videoUri)

            val durationStr = retriever.extractMetadata(
                MediaMetadataRetriever.METADATA_KEY_DURATION
            )
            val durationMs = durationStr?.toLongOrNull() ?: 0L

            if (durationMs <= 0L) {
                return@withContext emptyList()
            }

            val intervalMs = 1000L / fps
            var currentMs = 0L

            while (currentMs < durationMs) {

                val timeUs = currentMs * 1000L
                val rawBitmap = retriever.getFrameAtTime(
                    timeUs,
                    MediaMetadataRetriever.OPTION_CLOSEST_SYNC
                )

                if (rawBitmap != null) {
                    val scaledBitmap = scaleDownIfNeeded(rawBitmap, maxDimension)
                    frames.add(ExtractedFrame(timestampMs = currentMs, bitmap = scaledBitmap))
                }

                val progress = (currentMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
                onProgress(progress, currentMs, durationMs)

                currentMs += intervalMs
            }
        }catch (ex : Exception ){
            Log.e("VideoFrameExtractor",ex.message.toString())
        }
        finally {
            retriever.release()
        }
        frames
    }

    private fun scaleDownIfNeeded(bitmap: Bitmap, maxDim: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        if (width <= maxDim && height <= maxDim) return bitmap

        val scale = maxDim.toFloat() / maxOf(width, height).toFloat()
        val newWidth = (width * scale).toInt()
        val newHeight = (height * scale).toInt()
        return Bitmap.createScaledBitmap(
            bitmap,
            newWidth,
            newHeight,
            true
        )
    }
}
