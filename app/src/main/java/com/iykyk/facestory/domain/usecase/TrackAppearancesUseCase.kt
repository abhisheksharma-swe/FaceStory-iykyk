package com.iykyk.facestory.domain.usecase

import com.iykyk.facestory.domain.model.AppearanceSegment
import com.iykyk.facestory.domain.model.PersonCluster
import com.iykyk.facestory.util.Constants

class TrackAppearancesUseCase(
    private val gapThresholdMs: Long = Constants.APPEARANCE_GAP_THRESHOLD_MS
) {
    operator fun invoke(clusters: List<PersonCluster>): List<PersonCluster> {
        for (cluster in clusters) {
            cluster.appearances.clear()
            val sortedFaces = cluster.faces.sortedBy { it.timestampMs }

            if (sortedFaces.isEmpty()) continue

            var currentSegment: AppearanceSegment? = null

            for (face in sortedFaces) {
                if (currentSegment == null) {
                    currentSegment = AppearanceSegment(
                        personId = cluster.id,
                        startTimeMs = face.timestampMs,
                        endTimeMs = face.timestampMs,
                        faces = mutableListOf(face)
                    )
                } else {
                    val gap = face.timestampMs - currentSegment.endTimeMs
                    if (gap <= gapThresholdMs) {
                        currentSegment.endTimeMs = face.timestampMs
                        currentSegment.faces.add(face)
                    } else {
                        cluster.appearances.add(currentSegment)
                        currentSegment = AppearanceSegment(
                            personId = cluster.id,
                            startTimeMs = face.timestampMs,
                            endTimeMs = face.timestampMs,
                            faces = mutableListOf(face)
                        )
                    }
                }
            }

            currentSegment?.let { cluster.appearances.add(it) }
        }

        return clusters
    }
}
