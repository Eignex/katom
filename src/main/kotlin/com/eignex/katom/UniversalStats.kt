@file:OptIn(ExperimentalAtomicApi::class)

package com.eignex.katom

import kotlin.concurrent.atomics.*
import kotlin.time.Duration

interface UniversalStat<out T : Result> : SeriesStat<T>, PairedStat<T>,
    TimeStat<T>, VectorStat<T>, ResponseStat<T> {
    override fun update(
        x: Double, y: Double, weight: Double
    ) = update(x, weight)

    override fun update(
        value: Double, timestampNanos: Long, weight: Double
    ) = update(value, weight)

    override fun update(vector: DoubleArray, weight: Double) =
        update(0.0, weight)

    override fun update(
        x: DoubleArray, y: Double, weight: Double
    ) = update(0.0, weight)
}

class Count(
    override val name: String = "count"
) : UniversalStat<CountResult>, HasCount {

    private val _count = AtomicLong(0L)
    override val count: Long get() = _count.load()

    override fun update(value: Double, weight: Double) {
        _count.incrementAndFetch()
    }

    override fun read() = CountResult(name, count)
}


class TotalWeights(
    override val name: String = "totalWeights", sumOfWeights: Double = 0.0
) : UniversalStat<SumResult>, HasTotalWeights {

    private val _totalWeights = AtomicLong(sumOfWeights.toRawBits())
    override val totalWeights: Double get() = Double.fromBits(_totalWeights.load())

    override fun update(value: Double, weight: Double) {
        _totalWeights.addDouble(value * weight)
    }

    override fun read() = SumResult(name, totalWeights)
}

class EventRate(
    override val name: String = "eventRate", startTime: Long = -1L
) : UniversalStat<RateResult>, HasRate {

    private val _rate = Rate(name, startTime)

    override fun update(value: Double, weight: Double) {
        _rate.update(value, System.nanoTime())
    }

    override fun read() = _rate.read()

    override val rate: Double get() = _rate.rate
    override val timestampNanos: Long get() = _rate.timestampNanos
}

class DecayingEventRate(
    override val name: String = "decayingEventRate", halfLife: Duration
) : UniversalStat<RateResult> {
    private val rate = DecayingRate(halfLife)

    override fun update(value: Double, weight: Double) {
        rate.update(value, System.nanoTime())
    }

    override fun read() = rate.read()
}
