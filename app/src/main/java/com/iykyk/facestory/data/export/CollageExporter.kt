package com.iykyk.facestory.data.export

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.iykyk.facestory.util.UniversalLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class CollageExporter(private val context: Context) {

    companion object {
        private const val TAG = "CollageExporter"
    }

    suspend fun saveToGallery(bitmap: Bitmap): Uri? = withContext(Dispatchers.IO) {
        val filename = "FaceStory_${System.currentTimeMillis()}.png"
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, filename)
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/FaceStory")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
        }

        try {
            val resolver = context.contentResolver
            val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            if (uri == null) {
                UniversalLogger.e(TAG, "MediaStore insert returned null URI")
                return@withContext null
            }

            resolver.openOutputStream(uri)?.use { stream ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                values.clear()
                values.put(MediaStore.Images.Media.IS_PENDING, 0)
                resolver.update(uri, values, null, null)
            }

            uri
        } catch (e: Exception) {
            UniversalLogger.e(TAG, "Failed saving collage to gallery: ${e.message}", e)
            null
        }
    }
}
