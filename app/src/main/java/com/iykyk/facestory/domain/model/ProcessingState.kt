package com.iykyk.facestory.domain.model

import android.graphics.Bitmap

sealed interface ProcessingState {

    data object Idle : ProcessingState

    data class Processing(
        val progress: Float,
        val stage: String
    ): ProcessingState

    data class Success(
        val people: List<PersonCluster>,
        val collage: Bitmap? = null
    ): ProcessingState

    data class Error(
        val message: String,
    ): ProcessingState

}