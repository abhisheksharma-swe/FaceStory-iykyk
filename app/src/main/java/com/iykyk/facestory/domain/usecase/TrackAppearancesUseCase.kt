package com.iykyk.facestory.domain.usecase

import android.graphics.Rect
import com.iykyk.facestory.domain.model.AppearanceSegment
import com.iykyk.facestory.domain.model.DetectedFace
import com.iykyk.facestory.util.Constants
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

class TrackAppearancesUseCase(
    private val gapThresholdMs: Long = Constants.APPEARANCE_GAP_THRESHOLD_MS
) {
    companion object {
        private const val MAX_TRACKING_COST = 1.35f
        private const val GATED_COST = 1000.0f
    }

    operator fun invoke(allFaces: List<DetectedFace>): List<AppearanceSegment> {
        if (allFaces.isEmpty()) return emptyList()

        val framesByTimestamp = allFaces.groupBy { it.timestampMs }.toSortedMap()
        val completedSegments = mutableListOf<AppearanceSegment>()
        val activeSegments = mutableListOf<AppearanceSegment>()
        var nextSegmentId = 1

        for ((timestamp, facesInFrame) in framesByTimestamp) {
            val timedOut = activeSegments.filter { (timestamp - it.endTimeMs) > gapThresholdMs }
            completedSegments.addAll(timedOut)
            activeSegments.removeAll(timedOut.toSet())

            if (activeSegments.isEmpty()) {
                for (face in facesInFrame) {
                    val newSeg = AppearanceSegment(
                        id = nextSegmentId++,
                        startTimeMs = timestamp,
                        endTimeMs = timestamp,
                        candidateFaces = mutableListOf(face)
                    )
                    activeSegments.add(newSeg)
                }
                continue
            }

            val numTracks = activeSegments.size
            val numFaces = facesInFrame.size
            val costMatrix = Array(numTracks) { tIdx ->
                val track = activeSegments[tIdx]
                val lastFace = track.candidateFaces.last()
                val gapMs = timestamp - track.endTimeMs

                FloatArray(numFaces) { fIdx ->
                    val face = facesInFrame[fIdx]
                    computeSpatialCost(face, lastFace, gapMs)
                }
            }

            val assignments = HungarianMatcher.minCostAssignment(costMatrix)
            val matchedFaces = BooleanArray(numFaces)

            for (tIdx in 0 until numTracks) {
                val fIdx = assignments[tIdx]
                if (fIdx in 0 until numFaces && costMatrix[tIdx][fIdx] <= MAX_TRACKING_COST) {
                    val track = activeSegments[tIdx]
                    val face = facesInFrame[fIdx]
                    track.endTimeMs = timestamp
                    track.candidateFaces.add(face)
                    matchedFaces[fIdx] = true
                }
            }

            for (fIdx in 0 until numFaces) {
                if (!matchedFaces[fIdx]) {
                    val face = facesInFrame[fIdx]
                    val newSeg = AppearanceSegment(
                        id = nextSegmentId++,
                        startTimeMs = timestamp,
                        endTimeMs = timestamp,
                        candidateFaces = mutableListOf(face)
                    )
                    activeSegments.add(newSeg)
                }
            }
        }

        completedSegments.addAll(activeSegments)
        completedSegments.sortBy { it.startTimeMs }
        return completedSegments
    }

    private fun computeSpatialCost(face: DetectedFace, lastFace: DetectedFace, gapMs: Long): Float {
        val frameW = face.frameWidth.toFloat().coerceAtLeast(1f)
        val frameH = face.frameHeight.toFloat().coerceAtLeast(1f)
        val frameDiag = sqrt(frameW * frameW + frameH * frameH)

        val c1x = face.boundingBox.centerX().toFloat()
        val c1y = face.boundingBox.centerY().toFloat()
        val c2x = lastFace.boundingBox.centerX().toFloat()
        val c2y = lastFace.boundingBox.centerY().toFloat()

        val dx = c1x - c2x
        val dy = c1y - c2y
        val centerDist = sqrt(dx * dx + dy * dy) / frameDiag

        val iou = calculateIoU(face.boundingBox, lastFace.boundingBox)

        val area1 = (face.boundingBox.width() * face.boundingBox.height()).toFloat().coerceAtLeast(1f)
        val area2 = (lastFace.boundingBox.width() * lastFace.boundingBox.height()).toFloat().coerceAtLeast(1f)
        val sizeRatio = max(area1, area2) / min(area1, area2)

        if (centerDist > Constants.MAX_SPATIAL_TRACK_DISTANCE && iou == 0f) {
            return GATED_COST
        }
        if (sizeRatio > 3.5f && iou == 0f) {
            return GATED_COST
        }

        val temporalFactor = 1.0f + (gapMs.toFloat() / 1000f) * 0.15f
        val baseCost = (centerDist * 1.8f) + ((1.0f - iou) * 0.6f) + ((sizeRatio - 1.0f).coerceAtLeast(0f) * 0.15f)
        return baseCost * temporalFactor
    }

    private fun calculateIoU(box1: Rect, box2: Rect): Float {
        val left = max(box1.left, box2.left)
        val top = max(box1.top, box2.top)
        val right = min(box1.right, box2.right)
        val bottom = min(box1.bottom, box2.bottom)

        val intersectionArea = max(0, right - left) * max(0, bottom - top)
        val box1Area = box1.width() * box1.height()
        val box2Area = box2.width() * box2.height()
        val unionArea = box1Area + box2Area - intersectionArea

        return if (unionArea > 0) intersectionArea.toFloat() / unionArea.toFloat() else 0f
    }

    internal object HungarianMatcher {
        fun minCostAssignment(costMatrix: Array<FloatArray>): IntArray {
            val rows = costMatrix.size
            if (rows == 0) return IntArray(0)
            val cols = costMatrix[0].size
            if (cols == 0) return IntArray(rows) { -1 }

            val dim = max(rows, cols)
            val matrix = Array(dim) { r ->
                FloatArray(dim) { c ->
                    if (r < rows && c < cols) costMatrix[r][c] else 0f
                }
            }

            for (r in 0 until dim) {
                var minVal = matrix[r][0]
                for (c in 1 until dim) if (matrix[r][c] < minVal) minVal = matrix[r][c]
                if (minVal > 0f) {
                    for (c in 0 until dim) matrix[r][c] -= minVal
                }
            }

            for (c in 0 until dim) {
                var minVal = matrix[0][c]
                for (r in 1 until dim) if (matrix[r][c] < minVal) minVal = matrix[r][c]
                if (minVal > 0f) {
                    for (r in 0 until dim) matrix[r][c] -= minVal
                }
            }

            val u = FloatArray(dim + 1)
            val v = FloatArray(dim + 1)
            val p = IntArray(dim + 1)
            val way = IntArray(dim + 1)

            for (i in 1..dim) {
                p[0] = i
                var j0 = 0
                val minv = FloatArray(dim + 1) { Float.MAX_VALUE }
                val used = BooleanArray(dim + 1)

                do {
                    used[j0] = true
                    val i0 = p[j0]
                    var delta = Float.MAX_VALUE
                    var j1 = 0

                    for (j in 1..dim) {
                        if (!used[j]) {
                            val cur = matrix[i0 - 1][j - 1] - u[i0] - v[j]
                            if (cur < minv[j]) {
                                minv[j] = cur
                            }
                            if (minv[j] < delta) {
                                delta = minv[j]
                                j1 = j
                            }
                        }
                    }

                    for (j in 0..dim) {
                        if (used[j]) {
                            u[p[j]] += delta
                            v[j] -= delta
                        } else {
                            minv[j] -= delta
                        }
                    }
                    j0 = j1
                } while (p[j0] != 0)

                do {
                    val j1 = way[j0]
                    p[j0] = p[j1]
                    j0 = j1
                } while (j0 != 0)
            }

            val result = IntArray(rows) { -1 }
            for (j in 1..dim) {
                val i = p[j]
                if (i in 1..rows && j in 1..cols) {
                    result[i - 1] = j - 1
                }
            }
            return result
        }
    }
}
