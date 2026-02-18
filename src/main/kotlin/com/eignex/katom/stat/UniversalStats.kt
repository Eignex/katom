package com.eignex.katom.stat

import com.eignex.katom.concurrent.SerialMode
import com.eignex.katom.concurrent.StreamMode
import com.eignex.katom.concurrent.defaultStreamMode
import com.eignex.katom.concurrent.getValue
import com.eignex.katom.core.*
import kotlin.time.Duration

interface UniversalStat<T : Result> :
    SeriesStat<T>,
    PairedStat<T>,
    TimeStat<T>,
    VectorStat<T> {
    override fun update(
        x: Double, y: Double, weight: Double
    ) = update(x, weight)

    override fun update(
        value: Double, nanos: Long, weight: Double
    ) = update(value, weight)

    override fun update(vector: DoubleArray, weight: Double) =
        update(0.0, weight)
}

class Count(
    override val mode: StreamMode = defaultStreamMode,
    override val name: String? = null
) : UniversalStat<CountResult>, HasCount {

    private val _count = mode.newLong(0L)
    override val count: Long by _count

    override fun update(value: Double, weight: Double) {
        _count.add(1L)
    }

    override fun merge(values: CountResult) {
        _count.add(values.count)
    }

    override fun reset() {
        _count.store(0L)
    }

    override fun read() = CountResult(count, name)
}


class TotalWeights(
    override val mode: StreamMode = SerialMode,
    override val name: String? = null
) : UniversalStat<SumResult>, HasTotalWeights {

    private val _totalWeights = mode.newDouble(0.0)
    override val totalWeights: Double by _totalWeights

    override fun update(value: Double, weight: Double) {
        _totalWeights.add(weight)
    }

    override fun merge(values: SumResult) {
        _totalWeights.add(values.sum)
    }

    override fun reset() {
        _totalWeights.store(0.0)
    }

    override fun read() = SumResult(totalWeights, name)
}

class EventRate(
    override val mode: StreamMode = defaultStreamMode,
    override val name: String? = null
) : UniversalStat<RateResult>, HasRate {

    private val _rate = Rate(mode, name)

    override fun update(value: Double, weight: Double) {
        _rate.update(1.0, System.nanoTime())
    }

    override fun merge(values: RateResult) {
        _rate.merge(values)
    }

    override fun reset() {
        _rate.reset()
    }

    override fun read() = _rate.read()

    override val rate: Double get() = _rate.rate
    override val timestampNanos: Long get() = _rate.timestampNanos
}

class DecayingEventRate(
    halfLife: Duration,
    override val mode: StreamMode = defaultStreamMode,
    override val name: String? = null
) : UniversalStat<DecayingRateResult>, HasRate {
    private val _rate = DecayingRate(halfLife, mode, name)

    override fun update(value: Double, weight: Double) {
        _rate.update(1.0, System.nanoTime())
    }

    override fun merge(values: DecayingRateResult) {
        _rate.merge(values)
    }

    override fun reset() {
        _rate.reset()
    }

    override fun read() = _rate.read()
    override val rate: Double get() = _rate.rate
    override val timestampNanos: Long get() = _rate.timestampNanos
}
