package com.iykyk.facestory.domain.model

import android.graphics.Bitmap
import com.iykyk.facestory.domain.usecase.ProcessingDiagnostics

sealed interface ProcessingState {

    data object Idle : ProcessingState

    data class Processing(
        val progress: Float,
        val stage: String
    ) : ProcessingState

    data class Success(
        val people: List<PersonCluster>,
        val collage: Bitmap? = null,
        val diagnostics: ProcessingDiagnostics? = null
    ) : ProcessingState

    data class Error(
        val message: String
    ) : ProcessingState

    data class NoFacesFound(
        val framesScanned: Int,
        val blurryFramesSkipped: Int
    ) : ProcessingState
}
