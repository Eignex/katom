package com.eignex.katom.concurrent

import com.eignex.katom.core.*
import com.eignex.katom.stat.UniversalStat
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

fun <R : Result> SeriesStat<R>.locked() = LockedSeriesStat(this)
fun <R : Result> TimeStat<R>.locked() = LockedTimeStat(this)
fun <R : Result> PairedStat<R>.locked() = LockedPairedStat(this)
fun <R : Result> VectorStat<R>.locked() = LockedVectorStat(this)
fun <R : Result> UniversalStat<R>.locked() = LockedUniversalStat(this)

class LockedSeriesStat<R : Result>(private val delegate: SeriesStat<R>) :
    SeriesStat<R> by delegate {
    private val lock = ReentrantReadWriteLock()

    override fun update(value: Double, weight: Double) {
        lock.write { delegate.update(value, weight) }
    }

    override fun merge(values: R) {
        lock.write { delegate.merge(values) }
    }

    override fun reset() {
        lock.write { delegate.reset() }
    }

    override fun read(): R {
        return lock.read { delegate.read() }
    }
}

class LockedTimeStat<R : Result>(private val delegate: TimeStat<R>) :
    TimeStat<R> by delegate {
    private val lock = ReentrantReadWriteLock()

    override fun update(value: Double, nanos: Long, weight: Double) {
        lock.write { delegate.update(value, nanos, weight) }
    }

    override fun merge(values: R) {
        lock.write { delegate.merge(values) }
    }

    override fun reset() {
        lock.write { delegate.reset() }
    }

    override fun read(): R {
        return lock.read { delegate.read() }
    }
}

class LockedStat<R : Result>(private val delegate: SeriesStat<R>) :
    SeriesStat<R> by delegate {
    private val lock = ReentrantReadWriteLock()

    override fun update(value: Double, weight: Double) {
        lock.write { delegate.update(value, weight) }
    }

    override fun merge(values: R) {
        lock.write { delegate.merge(values) }
    }

    override fun reset() {
        lock.write { delegate.reset() }
    }

    override fun read(): R {
        return lock.read { delegate.read() }
    }
}

class LockedPairedStat<R : Result>(private val delegate: PairedStat<R>) :
    PairedStat<R> by delegate {
    private val lock = ReentrantReadWriteLock()

    override fun update(x: Double, y: Double, weight: Double) {
        lock.write { delegate.update(x, y, weight) }
    }

    override fun merge(values: R) {
        lock.write { delegate.merge(values) }
    }

    override fun reset() {
        lock.write { delegate.reset() }
    }

    override fun read(): R {
        return lock.read { delegate.read() }
    }
}

class LockedVectorStat<R : Result>(private val delegate: VectorStat<R>) :
    VectorStat<R> by delegate {
    private val lock = ReentrantReadWriteLock()

    override fun update(vector: DoubleArray, weight: Double) {
        lock.write { delegate.update(vector, weight) }
    }

    override fun merge(values: R) {
        lock.write { delegate.merge(values) }
    }

    override fun reset() {
        lock.write { delegate.reset() }
    }

    override fun read(): R {
        return lock.read { delegate.read() }
    }
}

class LockedUniversalStat<R : Result>(private val delegate: UniversalStat<R>) :
    UniversalStat<R> by delegate {
    private val lock = ReentrantReadWriteLock()

    override fun update(value: Double, weight: Double) {
        lock.write { delegate.update(value, weight) }
    }

    override fun merge(values: R) {
        lock.write { delegate.merge(values) }
    }

    override fun reset() {
        lock.write { delegate.reset() }
    }

    override fun read(): R {
        return lock.read { delegate.read() }
    }
}
