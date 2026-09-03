package com.iykyk.facestory.ui.viewmodel

import android.app.Application
import android.graphics.Bitmap
import android.net.Uri
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.iykyk.facestory.data.export.CollageExporter
import com.iykyk.facestory.data.export.ShareManager
import com.iykyk.facestory.data.ml.MLKitFaceDetector
import com.iykyk.facestory.data.ml.TFLiteFaceEmbedder
import com.iykyk.facestory.data.video.VideoFrameExtractor
import com.iykyk.facestory.domain.model.ProcessingState
import com.iykyk.facestory.domain.usecase.ProcessVideoUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class CollageViewModel(application: Application) : AndroidViewModel(application) {

    private val _state = MutableStateFlow<ProcessingState>(ProcessingState.Idle)
    val state: StateFlow<ProcessingState> = _state.asStateFlow()

    private val extractor = VideoFrameExtractor(application)
    private val detector = MLKitFaceDetector()
    private val embedder = TFLiteFaceEmbedder(application)
    private val processVideoUseCase = ProcessVideoUseCase(extractor, detector, embedder)

    private val exporter = CollageExporter(application)
    private val shareManager = ShareManager(application)

    fun selectVideo(uri: Uri) {
        viewModelScope.launch {
            _state.value = ProcessingState.Processing(0f, "Starting video analysis...")
            try {
                val result = processVideoUseCase(uri) { progress, stage ->
                    _state.value = ProcessingState.Processing(progress, stage)
                }
                _state.value = ProcessingState.Success(result.people, result.collage)
            } catch (e: Exception) {
                _state.value = ProcessingState.Error(e.localizedMessage ?: "Processing failed")
            }
        }
    }

    fun saveCollage(bitmap: Bitmap) {
        viewModelScope.launch {
            val uri = exporter.saveToGallery(bitmap)
            if (uri != null) {
                Toast.makeText(getApplication(), "Saved to Pictures/FaceStory!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(getApplication(), "Failed to save image", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun shareCollage(bitmap: Bitmap) {
        viewModelScope.launch {
            shareManager.shareBitmap(bitmap)
        }
    }

    fun reset() {
        _state.value = ProcessingState.Idle
    }

    override fun onCleared() {
        super.onCleared()
        detector.close()
        embedder.close()
    }
}
