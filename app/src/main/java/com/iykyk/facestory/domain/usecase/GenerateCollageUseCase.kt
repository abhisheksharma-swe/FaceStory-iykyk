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
        require(people.isNotEmpty()) { "Cannot render a collage with no identity clusters." }
        require(people.map { it.id }.distinct().size == people.size) {
            "Each collage tile must map to one unique identity cluster."
        }
        require(people.all { it.appearances.isNotEmpty() && it.representativeFace != null }) {
            "Every identity cluster must have one selected representative before rendering."
        }
        renderer.render(people)
    }
}
