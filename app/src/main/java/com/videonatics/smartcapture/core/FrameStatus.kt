package com.videonatics.smartcapture.core

data class FrameStats(
    val min: Long,
    val max: Long,
    val mean: Double,
    val variance: Double,
    val stdDev: Double
)

fun calculateStats(times: List<Long>): FrameStats {
    require(times.isNotEmpty())

    val n = times.size

    var sum = 0.0
    var min = Long.MAX_VALUE
    var max = Long.MIN_VALUE

    // First pass → min, max, sum
    for (t in times) {
        sum += t
        if (t < min) min = t
        if (t > max) max = t
    }

    val mean = sum / n

    // Second pass → variance
    var varianceSum = 0.0
    for (t in times) {
        val diff = t - mean
        varianceSum += diff * diff
    }

    val variance = varianceSum / n
    val stdDev = kotlin.math.sqrt(variance)

    return FrameStats(min, max, mean, variance, stdDev)
}