package com.iykyk.facestory

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import com.iykyk.facestory.domain.model.AppearanceSegment
import com.iykyk.facestory.domain.usecase.ClusterFacesUseCase
import com.iykyk.facestory.domain.usecase.TrackAppearancesUseCase
import com.iykyk.facestory.util.MathUtils

class ExampleUnitTest {
    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }

    @Test
    fun hungarianMatcherSolvesAssignmentCorrectly() {
        val costMatrix = arrayOf(
            floatArrayOf(0.1f, 0.9f, 0.8f),
            floatArrayOf(0.9f, 0.2f, 0.7f),
            floatArrayOf(0.8f, 0.7f, 0.15f)
        )
        val assignment = TrackAppearancesUseCase.HungarianMatcher.minCostAssignment(costMatrix)
        assertEquals(0, assignment[0])
        assertEquals(1, assignment[1])
        assertEquals(2, assignment[2])
    }

    @Test
    fun sampleLikeAppearancesBecomeFivePeopleAndKeepAllTwentyAppearances() {
        val segments = buildList {
            for (appearance in 0 until 4) {
                for (person in 0 until 5) {
                    val embedding = FloatArray(5) { dimension ->
                        when (dimension) {
                            person -> 1f
                            (person + 1) % 5 -> 0.08f * (appearance + 1)
                            else -> 0f
                        }
                    }
                    add(
                        AppearanceSegment(
                            id = size + 1,
                            startTimeMs = (appearance * 10_000L) + person * 500L,
                            endTimeMs = (appearance * 10_000L) + person * 500L + 500L,
                            embedding = MathUtils.l2Normalize(embedding)
                        )
                    )
                }
            }
        }

        val people = ClusterFacesUseCase()(segments)

        assertEquals(5, people.size)
        assertEquals(20, people.sumOf { it.appearanceCount })
        people.forEach { assertEquals(4, it.appearanceCount) }
    }

    @Test
    fun simultaneousAppearancesAreNeverMergedIntoOneIdentity() {
        val first = AppearanceSegment(1, 0L, 250L, embedding = floatArrayOf(1f, 0f))
        val second = AppearanceSegment(2, 0L, 250L, embedding = MathUtils.l2Normalize(floatArrayOf(0.99f, 0.14f)))

        val people = ClusterFacesUseCase()(listOf(first, second))

        assertEquals(2, people.size)
        assertEquals(2, people.sumOf { it.appearanceCount })
    }

    @Test
    fun peopleSharingOnlyOneSampledFrameAreNeverMerged() {
        val first = AppearanceSegment(1, 1_000L, 1_000L, embedding = floatArrayOf(1f, 0f))
        val second = AppearanceSegment(
            2, 1_000L, 1_000L,
            embedding = MathUtils.l2Normalize(floatArrayOf(0.99f, 0.14f))
        )

        val people = ClusterFacesUseCase()(listOf(first, second))

        assertEquals(2, people.size)
        assertEquals(2, people.sumOf { it.appearanceCount })
    }

    @Test
    fun agreeingEmbeddingSamplesReuniteAChangedAppearance() {
        val first = AppearanceSegment(
            id = 1, startTimeMs = 0L, endTimeMs = 500L,
            embedding = MathUtils.l2Normalize(floatArrayOf(0.72f, 0.69f)),
            embeddingSamples = listOf(
                MathUtils.l2Normalize(floatArrayOf(0.98f, 0.20f)),
                MathUtils.l2Normalize(floatArrayOf(0.96f, 0.28f)),
                MathUtils.l2Normalize(floatArrayOf(0.97f, 0.24f))
            )
        )
        val second = AppearanceSegment(
            id = 2, startTimeMs = 2_000L, endTimeMs = 2_500L,
            embedding = MathUtils.l2Normalize(floatArrayOf(0.68f, 0.73f)),
            embeddingSamples = listOf(
                MathUtils.l2Normalize(floatArrayOf(0.99f, 0.14f)),
                MathUtils.l2Normalize(floatArrayOf(0.95f, 0.31f)),
                MathUtils.l2Normalize(floatArrayOf(0.98f, 0.20f))
            )
        )

        val people = ClusterFacesUseCase()(listOf(first, second))

        assertEquals(1, people.size)
        assertEquals(2, people.single().appearanceCount)
    }

    @Test
    fun clusteringNeverLeavesValidNonOverlappingCandidateAboveThreshold() {
        val first = AppearanceSegment(1, 0L, 500L, embedding = MathUtils.l2Normalize(floatArrayOf(1f, 0f, 0f)))
        val second = AppearanceSegment(2, 1000L, 1500L, embedding = MathUtils.l2Normalize(floatArrayOf(0.85f, 0.52f, 0f)))
        val third = AppearanceSegment(3, 2000L, 2500L, embedding = MathUtils.l2Normalize(floatArrayOf(0f, 1f, 0f)))

        val people = ClusterFacesUseCase(similarityThreshold = 0.58f)(listOf(first, second, third))

        assertEquals(2, people.size)
        val mergedCluster = people.first { it.appearanceCount == 2 }
        assertTrue(mergedCluster.appearances.any { it.id == 1 })
        assertTrue(mergedCluster.appearances.any { it.id == 2 })
    }
}
