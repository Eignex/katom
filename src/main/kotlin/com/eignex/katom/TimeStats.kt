package com.eignex.katom

import kotlin.math.exp
import kotlin.time.Duration

class Rate(
    override val mode: StreamMode = defaultStreamMode,
    override val name: String? = null
) : TimeStat<RateResult>, HasRate {

    private val _totalValues = mode.newDouble(0.0)

    @Volatile
    private var _startTime = Long.MIN_VALUE
    val startTime: Long get() = _startTime

    override fun update(
        value: Double,
        nanos: Long,
        weight: Double
    ) {
        if (_startTime == Long.MIN_VALUE) {
            _startTime = nanos
        }
        _totalValues.add(value * weight)
    }

    override fun read(): RateResult {
        val start = _startTime
        val now = System.nanoTime()

        if (start == Long.MIN_VALUE) {
            return RateResult(
                startTime = now,
                totalValue = 0.0,
                timestampNanos = now,
                name = name
            )
        }

        return RateResult(
            startTime = start,
            totalValue = _totalValues.load(),
            timestampNanos = now,
            name = name
        )
    }

    override fun merge(values: RateResult) {
        if (values.totalValue == 0.0) return // Nothing to merge

        _totalValues.add(values.totalValue)

        val currentStart = _startTime
        if (currentStart == Long.MIN_VALUE || values.startTime < currentStart) {
            _startTime = values.startTime
        }
    }

    override fun reset() {
        _startTime = Long.MIN_VALUE
        _totalValues.store(0.0)
    }

    override val rate: Double get() = read().rate
    override val timestampNanos: Long get() = System.nanoTime()
}

class DecayingRate(
    halfLife: Duration,
    override val mode: StreamMode = defaultStreamMode,
    override val name: String? = null,
) : TimeStat<DecayingRateResult>, HasRate {

    private val alpha = 0.69314718056 / halfLife.inWholeNanoseconds.toDouble()
    private val rotationThresholdNanos = halfLife.inWholeNanoseconds * 50

    private class Epoch(
        val landmarkNanos: Long,
        val accumulator: StreamDouble
    )

    private val epochRef = mode.newReference(
        Epoch(
            System.nanoTime(),
            mode.newDouble(0.0)
        )
    )

    override fun update(value: Double, nanos: Long, weight: Double) {
        while (true) {
            val currentEpoch = epochRef.load()

            if (nanos - currentEpoch.landmarkNanos > rotationThresholdNanos) {
                tryRotateEpoch(currentEpoch, nanos)
                continue
            }

            val dt = nanos - currentEpoch.landmarkNanos
            val scaleFactor = exp(alpha * dt)
            currentEpoch.accumulator.add(value * weight * scaleFactor)
            return
        }
    }

    private fun tryRotateEpoch(oldEpoch: Epoch, now: Long) {
        val oldVal = oldEpoch.accumulator.load()
        val dt = now - oldEpoch.landmarkNanos
        val carriedOverValue = oldVal * exp(-alpha * dt)

        val newAccumulator = mode.newDouble(carriedOverValue)
        val newEpoch = Epoch(now, newAccumulator)

        epochRef.compareAndSet(oldEpoch, newEpoch)
    }

    override fun read(): DecayingRateResult {
        val now = System.nanoTime()
        val currentEpoch = epochRef.load()
        val totalAccumulated = currentEpoch.accumulator.load()

        val dt = (now - currentEpoch.landmarkNanos).toDouble()
        val currentEnergy = totalAccumulated * exp(-alpha * dt)
        val ratePerSec = currentEnergy * alpha * 1_000_000_000.0

        return DecayingRateResult(ratePerSec, now, name)
    }

    override fun merge(values: DecayingRateResult) {
        if (values.rate <= 0.0) return

        while (true) {
            val currentEpoch = epochRef.load()
            val now = values.timestampNanos

            if (now - currentEpoch.landmarkNanos > rotationThresholdNanos) {
                tryRotateEpoch(currentEpoch, now)
                continue
            }

            val incomingEnergy = values.rate / (alpha * 1_000_000_000.0)
            val dt = (now - currentEpoch.landmarkNanos).toDouble()
            val scaledIncomingEnergy = incomingEnergy * exp(alpha * dt)

            currentEpoch.accumulator.add(scaledIncomingEnergy)
            break
        }
    }

    override fun reset() {
        epochRef.compareAndSet(
            epochRef.load(),
            Epoch(
                System.nanoTime(),
                mode.newDouble(0.0)
            )
        )
    }

    override val rate: Double get() = read().rate
    override val timestampNanos: Long get() = System.nanoTime()
}
