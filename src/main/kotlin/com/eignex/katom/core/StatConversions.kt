package com.eignex.katom.core

import com.eignex.katom.concurrent.StreamMode
import kotlin.math.exp
import kotlin.time.Duration


fun <R : Result> TimeStat<R>.atNow(): SeriesStat<R> = object : SeriesStat<R> {
    override fun update(value: Double, weight: Double) =
        this@atNow.update(value, System.nanoTime(), weight)

    override fun merge(values: R) = this@atNow.merge(values)
    override fun reset() = this@atNow.reset()
    override val name: String? get() = this@atNow.name
    override fun read(): R = this@atNow.read()
    override val mode: StreamMode get() = this@atNow.mode
}

class TimeDecayingStat<R : Result>(
    val delegate: SeriesStat<R>,
    val halfLife: Duration
) : TimeStat<R> {

    override val mode: StreamMode = delegate.mode
    override val name: String? = delegate.name

    private val alpha = 0.69314718056 / halfLife.inWholeNanoseconds.toDouble()

    // We use StreamRef<Long> because StreamLong does not currently expose compareAndSet.
    private val epochRef = mode.newReference(System.nanoTime())

    override fun update(value: Double, nanos: Long, weight: Double) {
        if (weight <= 0.0) return

        var currentEpoch = epochRef.load()

        while (nanos > currentEpoch) {
            if (epochRef.compareAndSet(currentEpoch, nanos)) {
                currentEpoch = nanos
                break
            }
            currentEpoch = epochRef.load()
        }

        val ageNanos = currentEpoch - nanos
        val decayedWeight = weight * exp(-alpha * ageNanos.toDouble())

        delegate.update(value, decayedWeight)
    }

    override fun merge(values: R) {
        delegate.merge(values)
    }

    override fun reset() {
        epochRef.store(System.nanoTime())
        delegate.reset()
    }

    override fun read(): R = delegate.read()
}

fun <R : Result> SeriesStat<R>.decaying(halfLife: Duration): TimeStat<R> {
    return TimeDecayingStat(this, halfLife)
}
