package com.iykyk.facestory.data.ml

import android.content.Context
import android.graphics.Bitmap
import com.iykyk.facestory.util.BitmapUtils
import com.iykyk.facestory.util.MathUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel

class TFLiteFaceEmbedder(
    context: Context,
    modelPath: String = "mobilefacenet.tflite"
) {
    private val interpreter: Interpreter
    private val inputSize = 112
    private val embeddingDim: Int

    init {
        val modelBuffer = loadModelFile(context, modelPath)
        val options = Interpreter.Options().apply {
            setNumThreads(4)
        }
        interpreter = Interpreter(modelBuffer, options)

        val outputShape = interpreter.getOutputTensor(0).shape()
        embeddingDim = outputShape[1]
    }

    suspend fun generateEmbedding(faceBitmap: Bitmap): FloatArray = withContext(Dispatchers.Default) {
        val resized = BitmapUtils.toEmbeddingInput(faceBitmap, inputSize)
        val byteBuffer = bitmapToByteBuffer(resized)

        val outputArray = Array(1) { FloatArray(embeddingDim) }
        interpreter.run(byteBuffer, outputArray)

        MathUtils.l2Normalize(outputArray[0])
    }

    private fun bitmapToByteBuffer(bitmap: Bitmap): ByteBuffer {
        val byteBuffer = ByteBuffer.allocateDirect(1 * inputSize * inputSize * 3 * 4)
        byteBuffer.order(ByteOrder.nativeOrder())

        val intValues = IntArray(inputSize * inputSize)
        bitmap.getPixels(intValues, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)

        var pixel = 0
        for (i in 0 until inputSize) {
            for (j in 0 until inputSize) {
                val value = intValues[pixel++]
                val r = ((value shr 16 and 0xFF) - 127.5f) / 128.0f
                val g = ((value shr 8 and 0xFF) - 127.5f) / 128.0f
                val b = ((value and 0xFF) - 127.5f) / 128.0f

                byteBuffer.putFloat(r)
                byteBuffer.putFloat(g)
                byteBuffer.putFloat(b)
            }
        }
        return byteBuffer
    }

    private fun loadModelFile(context: Context, modelPath: String): ByteBuffer {
        val fileDescriptor = context.assets.openFd(modelPath)
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        val startOffset = fileDescriptor.startOffset
        val declaredLength = fileDescriptor.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }

    fun close() {
        interpreter.close()
    }
}
