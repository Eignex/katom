@file:OptIn(ExperimentalAtomicApi::class)

package com.eignex.katom.concurrent

import java.util.concurrent.atomic.DoubleAdder as JDoubleAdder
import java.util.concurrent.atomic.LongAdder as JLongAdder
import kotlin.concurrent.atomics.AtomicLong as KAtomicLong
import kotlin.concurrent.atomics.AtomicReference as KAtomicReference
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.math.pow
import kotlin.reflect.KProperty

var DEFAULT_MODE: StreamMode = SerialMode

interface StreamMode {
    fun newDouble(initial: Double = 0.0): StreamDouble
    fun newLong(initial: Long = 0L): StreamLong
    fun <T> newReference(initial: T): StreamRef<T>
}

interface StreamDouble {
    fun load(): Double

    fun store(value: Double)

    fun add(delta: Double)

    fun addAndGet(delta: Double): Double
    fun getAndAdd(delta: Double): Double
}

interface StreamLong {
    fun load(): Long

    fun store(value: Long)

    fun add(delta: Long)

    fun addAndGet(delta: Long): Long
    fun getAndAdd(delta: Long): Long
}

interface StreamRef<T> {
    fun load(): T
    fun store(value: T)
    fun compareAndExchange(expectedValue: T, newValue: T): T
    fun compareAndSet(expectedValue: T, newValue: T): Boolean
}

operator fun StreamDouble.getValue(
    thisRef: Any?, property: KProperty<*>
): Double = load()

operator fun StreamLong.getValue(
    thisRef: Any?, property: KProperty<*>
): Long = load()

class FixedAtomicMode(precision: Int) : StreamMode {
    class Scaler(
        val precision: Int,
        val scale: Double = 10.0.pow(precision),
        val scaleLong: Long = 10.0.pow(precision).toLong()
    )

    val scaler = Scaler(precision)

    override fun newDouble(initial: Double) = FixedAtomicDouble(scaler)
    override fun newLong(initial: Long) = AtomicLong(initial)
    override fun <T> newReference(initial: T): StreamRef<T> =
        AtomicReference(initial)
}

object AtomicMode : StreamMode {
    override fun newDouble(initial: Double) = AtomicDouble(initial)
    override fun newLong(initial: Long) = AtomicLong(initial)
    override fun <T> newReference(initial: T) = AtomicReference<T>(initial)
}

object SerialMode : StreamMode {
    override fun newDouble(initial: Double) = SerialDouble(initial)
    override fun newLong(initial: Long) = SerialLong(initial)
    override fun <T> newReference(initial: T) = SerialRef(initial)
}

object AdderMode : StreamMode {
    override fun newDouble(initial: Double) = DoubleAdder(initial)
    override fun newLong(initial: Long) = LongAdder(initial)
    override fun <T> newReference(initial: T) = AtomicReference(initial)
}

class SerialLong(var ref: Long) : StreamLong {
    override fun load(): Long = ref

    override fun store(value: Long) {
        ref = value
    }

    override fun add(delta: Long) {
        ref += delta
    }

    override fun addAndGet(delta: Long): Long {
        ref += delta
        return delta
    }

    override fun getAndAdd(delta: Long): Long {
        val ret = ref
        ref += delta
        return ret
    }
}

class SerialDouble(var ref: Double) : StreamDouble {
    override fun load(): Double = ref

    override fun store(value: Double) {
        ref = value
    }

    override fun add(delta: Double) {
        ref += delta
    }

    override fun addAndGet(delta: Double): Double {
        ref += delta
        return ref
    }

    override fun getAndAdd(delta: Double): Double {
        val ret = ref
        ref += delta
        return ret
    }
}


class SerialRef<T>(var ref: T) : StreamRef<T> {
    override fun load(): T = ref

    override fun store(value: T) {
        ref = value
    }

    override fun compareAndExchange(expectedValue: T, newValue: T): T {
        if (ref === expectedValue) {
            ref = newValue
            return expectedValue
        } else {
            return ref
        }
    }

    override fun compareAndSet(expectedValue: T, newValue: T): Boolean {
        if (ref === expectedValue) {
            ref = newValue
            return true
        } else {
            return false
        }
    }
}

class FixedAtomicDouble(
    val scaler: FixedAtomicMode.Scaler, val ref: KAtomicLong
) : StreamDouble {

    constructor(
        scaler: FixedAtomicMode.Scaler, initial: Double = 0.0
    ) : this(
        scaler, KAtomicLong((initial * scaler.scaleLong).toLong())
    )

    override fun load(): Double = ref.load() / scaler.scale

    override fun store(value: Double) =
        ref.store((value * scaler.scaleLong).toLong())

    override fun add(delta: Double) {
        ref.addAndFetch((delta * scaler.scaleLong).toLong())
    }

    override fun addAndGet(delta: Double): Double {
        return ref.addAndFetch((delta * scaler.scaleLong).toLong()) / scaler.scale
    }

    override fun getAndAdd(delta: Double): Double {
        return ref.fetchAndAdd((delta * scaler.scaleLong).toLong()) / scaler.scale
    }
}

@JvmInline
value class AtomicDouble(val ref: KAtomicLong) : StreamDouble {

    constructor(initial: Double = 0.0) : this(KAtomicLong(initial.toRawBits()))

    override fun load() = Double.fromBits(ref.load())

    override fun store(value: Double) {
        ref.store(value.toRawBits())
    }

    override fun add(delta: Double) {
        var currentBits = ref.load()
        while (true) {
            val currentVal = Double.fromBits(currentBits)
            val nextBits = (currentVal + delta).toRawBits()
            val witness = ref.compareAndExchange(currentBits, nextBits)

            if (witness == currentBits) break // Success!
            currentBits = witness // Failed, try again with the new value
        }
    }

    override fun addAndGet(delta: Double): Double {
        var currentBits = ref.load()
        while (true) {
            val nextVal = Double.fromBits(currentBits) + delta
            val witness =
                ref.compareAndExchange(currentBits, nextVal.toRawBits())

            if (witness == currentBits) return nextVal
            currentBits = witness
        }
    }

    override fun getAndAdd(delta: Double): Double {
        var currentBits = ref.load()
        while (true) {
            val currentVal = Double.fromBits(currentBits)
            val nextBits = (currentVal + delta).toRawBits()
            val witness = ref.compareAndExchange(currentBits, nextBits)

            if (witness == currentBits) return currentVal
            currentBits = witness
        }
    }
}

@JvmInline
value class AtomicLong(val ref: KAtomicLong) : StreamLong {
    constructor(initial: Long = 0L) : this(KAtomicLong(initial))

    override fun load(): Long = ref.load()

    override fun store(value: Long) {
        ref.store(value)
    }

    override fun add(delta: Long) {
        ref.addAndFetch(delta)
    }

    override fun addAndGet(delta: Long): Long {
        return ref.addAndFetch(delta)
    }

    override fun getAndAdd(delta: Long): Long {
        return ref.fetchAndAdd(delta)
    }
}

@JvmInline
value class AtomicReference<T>(val ref: KAtomicReference<T>) : StreamRef<T> {
    constructor(value: T) : this(KAtomicReference(value))

    override fun load(): T = ref.load()

    override fun store(value: T) {
        ref.store(value)
    }

    override fun compareAndExchange(expectedValue: T, newValue: T): T {
        return ref.compareAndExchange(expectedValue, newValue)
    }

    override fun compareAndSet(expectedValue: T, newValue: T): Boolean {
        return ref.compareAndSet(expectedValue, newValue)
    }
}

@JvmInline
value class DoubleAdder(val ref: JDoubleAdder) : StreamDouble {

    constructor(initial: Double = 0.0) : this(JDoubleAdder().also {
        it.add(initial)
    })

    override fun load(): Double {
        return ref.sum()
    }

    override fun store(value: Double) {
        ref.reset()
        ref.add(value)
    }

    override fun add(delta: Double) {
        ref.add(delta)
    }

    override fun addAndGet(delta: Double): Double {
        ref.add(delta)
        return ref.sum()
    }

    override fun getAndAdd(delta: Double): Double {
        val ret = ref.sum()
        ref.add(delta)
        return ret
    }
}

@JvmInline
value class LongAdder(val ref: JLongAdder) : StreamLong {
    constructor(initial: Long = 0L) : this(JLongAdder().also { it.add(initial) })

    override fun load(): Long {
        return ref.sum()
    }

    override fun store(value: Long) {
        ref.reset()
        ref.add(value)
    }

    override fun add(delta: Long) {
        ref.add(delta)
    }

    override fun addAndGet(delta: Long): Long {
        ref.add(delta)
        return ref.sum()
    }

    override fun getAndAdd(delta: Long): Long {
        val ret = ref.sum()
        ref.add(delta)
        return ret
    }
}
