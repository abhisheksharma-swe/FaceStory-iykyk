package com.iykyk.facestory.data.ml

import android.content.Context
import android.graphics.Bitmap
import com.iykyk.facestory.util.BitmapUtils
import com.iykyk.facestory.util.MathUtils
import com.iykyk.facestory.util.UniversalLogger
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
    companion object {
        private const val TAG = "TFLiteFaceEmbedder"
    }

    private val interpreter: Interpreter
    private val inputHeight: Int
    private val inputWidth: Int
    private val batchSize: Int
    private val embeddingDim: Int
    private val inputTensorBytes: Int
    private val outputBatchSize: Int

    init {
        try {
            val modelBuffer = loadModelFile(context, modelPath)
            val options = Interpreter.Options().apply {
                setNumThreads(4)
            }
            interpreter = Interpreter(modelBuffer, options)

            val inputTensor = interpreter.getInputTensor(0)
            val inputShape = inputTensor.shape()
            batchSize = inputShape.getOrElse(0) { 1 }
            inputHeight = inputShape.getOrElse(1) { 112 }
            inputWidth = inputShape.getOrElse(2) { 112 }
            inputTensorBytes = inputTensor.numBytes()

            val outputTensor = interpreter.getOutputTensor(0)
            val outputShape = outputTensor.shape()
            outputBatchSize = outputShape.getOrElse(0) { 1 }
            embeddingDim = outputShape.lastOrNull() ?: 128
        } catch (e: Exception) {
            UniversalLogger.e(TAG, "Failed to initialize TFLite model $modelPath: ${e.message}", e)
            throw e
        }
    }

    suspend fun generateEmbedding(faceBitmap: Bitmap): FloatArray = withContext(Dispatchers.Default) {
        try {
            val resized = BitmapUtils.toEmbeddingInput(faceBitmap, targetSize = inputWidth)
            val byteBuffer = bitmapToByteBuffer(resized)

            val outputArray = Array(outputBatchSize) { FloatArray(embeddingDim) }
            interpreter.run(byteBuffer, outputArray)

            MathUtils.l2Normalize(outputArray[0])
        } catch (e: Exception) {
            UniversalLogger.e(TAG, "Failed generating embedding: ${e.message}", e)
            throw e
        }
    }

    private fun bitmapToByteBuffer(bitmap: Bitmap): ByteBuffer {
        val byteBuffer = ByteBuffer.allocateDirect(inputTensorBytes)
        byteBuffer.order(ByteOrder.nativeOrder())

        val intValues = IntArray(inputWidth * inputHeight)
        bitmap.getPixels(intValues, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)

        for (b in 0 until batchSize) {
            var pixel = 0
            for (i in 0 until inputHeight) {
                for (j in 0 until inputWidth) {
                    val value = intValues[pixel++]
                    val r = ((value shr 16 and 0xFF) - 127.5f) / 128.0f
                    val g = ((value shr 8 and 0xFF) - 127.5f) / 128.0f
                    val bVal = ((value and 0xFF) - 127.5f) / 128.0f

                    byteBuffer.putFloat(r)
                    byteBuffer.putFloat(g)
                    byteBuffer.putFloat(bVal)
                }
            }
        }
        byteBuffer.rewind()
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
