@file:OptIn(ExperimentalAtomicApi::class)

package com.eignex.katom

import kotlin.concurrent.atomics.AtomicLong
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.math.pow
import kotlin.math.sqrt

class Sum(override val name: String = "sum", sum: Double = 0.0) :
    SeriesStat<SumResult>, HasSum {

    private val _sum = AtomicLong(sum.toRawBits())
    override val sum: Double get() = Double.fromBits(_sum.load())

    override fun update(
        value: Double, weight: Double
    ) {
        _sum.addDouble(value * weight)
    }

    override fun read() = SumResult(name, sum)
}

class Mean(
    override val name: String = "mean",
    mean: Double = 0.0,
    totalWeight: Double = 0.0
) : SeriesStat<WeightedMeanResult>,
    HasTotalWeights, HasMean {

    private val _w = AtomicLong(totalWeight.toRawBits())
    private val _m1 = AtomicLong(mean.toRawBits())

    override val totalWeights: Double get() = _w.loadDouble()
    override val mean: Double get() = _m1.loadDouble()

    override fun update(value: Double, weight: Double) {
        val oldMean = _m1.loadDouble()
        val nextW = _w.addDouble(weight)

        val delta = value - oldMean
        val r = delta * (weight / nextW)

        _m1.addDouble(r)
    }

    override fun read() = WeightedMeanResult(name, totalWeights, mean)
}

class Variance(
    override val name: String = "variance",
    totalWeights: Double = 0.0,
    mean: Double = 0.0,
    variance: Double = 0.0
) : SeriesStat<WeightedVarianceResult>, HasMean, HasUnbiasedVariance {
    private val _w = AtomicLong(totalWeights.toRawBits())
    private val _m1 = AtomicLong(mean.toRawBits())
    private val _m2 = AtomicLong((variance * totalWeights).toRawBits())

    override val totalWeights: Double get() = _w.loadDouble()
    override val mean: Double get() = _m1.loadDouble()
    override val variance: Double
        get() {
            val weight = _w.loadDouble()
            return if (weight > 1.0) _m2.loadDouble() / weight else 0.0
        }

    override fun update(value: Double, weight: Double) {
        val oldMean = _m1.loadDouble()
        val nextW = _w.addDouble(weight)

        val delta = value - oldMean
        val r = delta * (weight / nextW)

        _m1.addDouble(r)
        _m2.addDouble((nextW - weight) * delta * r)
    }

    override fun read() =
        WeightedVarianceResult(name, totalWeights, mean, variance)
}

class Moments(
    override val name: String = "moments",
    totalWeights: Double = 0.0,
    mean: Double = 0.0,
    variance: Double = 0.0,
    skew: Double = 0.0,
    kurtosis: Double = 0.0
) : SeriesStat<MomentsResult>, HasMean, HasUnbiasedVariance, HasShapeMoments {

    private val _w = AtomicLong(totalWeights.toRawBits())
    private val _m1 = AtomicLong(mean.toRawBits())
    private val _m2 = AtomicLong((variance * totalWeights).toRawBits())
    private val _m3 =
        AtomicLong((skew * totalWeights * sqrt(variance).pow(3)).toRawBits())
    private val _m4 =
        AtomicLong(((kurtosis + 3.0) * totalWeights * variance.pow(2)).toRawBits())

    override val totalWeights: Double get() = _w.loadDouble()
    override val mean: Double get() = _m1.loadDouble()
    override val variance: Double get() = if (totalWeights > 1) _m2.loadDouble() / totalWeights else 0.0
    override val skew: Double
        get() = if (variance > 0) (_m3.loadDouble() / totalWeights) / variance.pow(
            1.5
        ) else 0.0
    override val kurtosis: Double
        get() = if (variance > 0) (_m4.loadDouble() / totalWeights) / variance.pow(
            2.0
        ) - 3.0 else 0.0

    override fun update(value: Double, weight: Double) {
        val m1 = _m1.loadDouble()
        val m2 = _m2.loadDouble()
        val m3 = _m3.loadDouble()
        val nextW = _w.addDouble(weight)
        val oldW = nextW - weight

        val delta = value - m1
        val deltaW = delta * (weight / nextW)
        val deltaW2 = deltaW * deltaW
        val term1 = delta * deltaW * oldW

        _m1.addDouble(deltaW)
        _m4.addDouble(term1 * deltaW2 * (nextW * nextW - 3 * nextW + 3) + 6 * deltaW2 * m2 - 4 * deltaW * m3)
        _m3.addDouble(term1 * deltaW * (nextW - 2) - 3 * deltaW * m2)
        _m2.addDouble(term1)
    }

    override fun read() =
        MomentsResult(name, totalWeights, mean, variance, skew, kurtosis)
}

class DecayingMean(
    private val alpha: Double,
    override val name: String = "decayingMean",
    mean: Double = Double.NaN
) : SeriesStat<MeanResult>, HasMean {

    private val _mean = AtomicLong(mean.toRawBits())
    override val mean: Double
        get() = _mean.loadDouble().let { if (it.isNaN()) 0.0 else it }

    override fun update(value: Double, weight: Double) {
        val a = alpha * weight
        _mean.updateDouble { current ->
            if (current.isNaN()) {
                value
            } else {
                current + a * (value - current)
            }
        }
    }

    override fun read() = MeanResult(name, mean)
}

class DecayingVariance(
    private val alpha: Double,
    override val name: String = "decayingVariance",
    mean: Double = Double.NaN,
    variance: Double = 0.0
) : SeriesStat<VarianceResult>, HasMean, HasVariance {

    private val _mean = AtomicLong(mean.toRawBits())
    private val _var = AtomicLong(variance.toRawBits())

    override val mean: Double
        get() = _mean.loadDouble().let { if (it.isNaN()) 0.0 else it }

    override val variance: Double
        get() = _var.loadDouble()

    override fun update(value: Double, weight: Double) {
        val a = alpha * weight
        var oldMean: Double
        var diff: Double

        while (true) {
            val currentBits = _mean.load()
            oldMean = Double.fromBits(currentBits)

            if (oldMean.isNaN()) {
                if (_mean.compareAndSet(currentBits, value.toRawBits())) {
                    return
                }
            } else {
                val newMean = oldMean + a * (value - oldMean)
                if (_mean.compareAndSet(currentBits, newMean.toRawBits())) {
                    diff = value - oldMean
                    break
                }
            }
        }

        val incr = a * diff
        _var.updateDouble { currentVar ->
            (1.0 - a) * (currentVar + diff * incr)
        }
    }

    override fun read() = VarianceResult(name, mean, variance)
}
