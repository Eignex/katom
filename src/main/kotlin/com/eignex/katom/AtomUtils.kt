@file:OptIn(ExperimentalAtomicApi::class)
package com.eignex.katom

import kotlin.concurrent.atomics.AtomicLong
import kotlin.concurrent.atomics.ExperimentalAtomicApi

fun AtomicLong.addDouble(value: Double): Double {
    var current = load()
    while (true) {
        val next = Double.fromBits(current) + value
        val witness = compareAndExchange(current, next.toRawBits())
        if (witness == current) return next
        current = witness
    }
}

fun AtomicLong.loadDouble(): Double {
    return Double.fromBits(load())
}

/**
 * Updates the double value atomically using the transformation [block].
 */
fun AtomicLong.updateDouble(block: (Double) -> Double): Double {
    var current = load()
    while (true) {
        val next = block(Double.fromBits(current))
        val witness = compareAndExchange(current, next.toRawBits())
        if (witness == current) return next
        current = witness
    }
}

fun AtomicLong.updateMax(value: Long) {
    var current = load()
    while (value > current) {
        val witness = compareAndExchange(current, value)
        if (witness == current) return
        current = witness
    }
}
