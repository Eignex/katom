@file:OptIn(ExperimentalAtomicApi::class)
package com.eignex.katom

import kotlin.concurrent.atomics.AtomicLong
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.math.exp
import kotlin.time.Duration


class Rate(override val name: String = "rate", startTime: Long = -1L) : TimeStat<RateResult>, HasTotalWeights, HasRate {

    private val _weights = AtomicLong(0L)
    override val totalWeights: Double get() = _weights.loadDouble()

    private val _startTime = AtomicLong(startTime)
    private val _lastTime = AtomicLong(-1L)

    val startTime: Long get() = _startTime.load()
    val lastTime: Long get() = _lastTime.load()

    override fun read(): RateResult {
        val start = startTime
        val end = lastTime

        if (start == -1L || end <= start) {
            return RateResult(name, 0.0, System.nanoTime())
        }

        val durationSeconds = (end - start) / 1_000_000_000.0

        return RateResult(
            name,
            rate = totalWeights / durationSeconds,
            timestampNanos = end
        )
    }

    override fun update(
        value: Double,
        timestampNanos: Long,
        weight: Double
    ) {
        _weights.addDouble(value*weight)
        if (startTime == -1L) {
            _startTime.compareAndSet(-1L, timestampNanos)
        }
        _lastTime.updateMax(timestampNanos)
    }

    override val rate: Double get() = read().rate
    override val timestampNanos: Long get() = lastTime
}

class DecayingRate(
    halfLife: Duration,
    override val name: String = "decayingRate",
    initialLandmarkNanos: Long = System.nanoTime()
) : TimeStat<RateResult>, HasRate {

    private val alpha = 0.69314718056 / halfLife.inWholeNanoseconds.toDouble()

    // Rebase if the landmark is older than 4 half-lives to maintain precision.
    // e^x becomes very large quickly; keeping x < 3-5 keeps math "stable".
    private val rebaseThresholdNanos = halfLife.inWholeNanoseconds * 4

    private val landmarkNanos = AtomicLong(initialLandmarkNanos)
    private val accumulator = AtomicLong(0L)

    override fun update(
        value: Double,
        timestampNanos: Long,
        weight: Double
    ) {
        val start = landmarkNanos.load()
        val dt = timestampNanos - start

        // If landmark is too old, pull it forward before updating.
        if (dt !in 0..rebaseThresholdNanos) {
            rebase(timestampNanos)
            return update(value, timestampNanos, weight)
        }

        val scaleFactor = exp(alpha * dt)
        val contribution = value * weight * scaleFactor

        // Safety check for extreme outliers
        if (!contribution.isFinite()) {
            rebase(timestampNanos)
            return update(value, timestampNanos, weight)
        }

        accumulator.addDouble(contribution)
    }

    override fun read(): RateResult {
        val now = System.nanoTime()
        val start = landmarkNanos.load()
        val accValue = accumulator.loadDouble()

        val dt = (now - start).toDouble()
        val decayFactor = exp(-alpha * dt)
        val currentEnergy = accValue * decayFactor

        // Rate = Energy * Alpha * 1e9 (per second)
        val ratePerSecond = currentEnergy * alpha * 1_000_000_000.0

        return RateResult(
            name,
            rate = ratePerSecond,
            timestampNanos = now
        )
    }

    private fun rebase(newLandmark: Long) {
        val oldLandmark = landmarkNanos.load()
        val dt = (newLandmark - oldLandmark).toDouble()

        // Decay the current total to the new landmark's "now"
        val decayFactor = exp(-alpha * dt)

        accumulator.updateDouble { it * decayFactor }
        landmarkNanos.store(newLandmark)
    }

    override val rate: Double get() = read().rate
    override val timestampNanos: Long get() = System.nanoTime()
}
