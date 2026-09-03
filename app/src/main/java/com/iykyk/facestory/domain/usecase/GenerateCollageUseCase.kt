package com.iykyk.facestory.domain.usecase

import android.graphics.Bitmap
import com.iykyk.facestory.data.export.CollageRenderer
import com.iykyk.facestory.domain.model.PersonCluster
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GenerateCollageUseCase(
    private val renderer: CollageRenderer = CollageRenderer()
) {
    suspend operator fun invoke(people: List<PersonCluster>): Bitmap = withContext(Dispatchers.Default) {
        renderer.render(people)
    }
}
