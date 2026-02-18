package com.eignex.katom

import kotlin.time.Duration

/**
 * The base interface for all statistical accumulators.
 *
 * @param R The type of the result object produced by this statistic.
 */
interface Stat<R : Result> {
    /**
     * Merge stat results from another accumulator into this.
     */
    fun merge(values: R)

    /**
     * Reset stats to initial state.
     */
    fun reset()

    /**
     * Computes and returns the current state of the statistic.
     */
    fun read(): R

    val mode: StreamMode
    val name: String?
}

/**
 * Statistics derived from a single series of numerical values.
 *
 * **Covers:** Distributions (Mean, Variance, Skewness), Quantiles (P50, P99), Min/Max.
 */
interface SeriesStat<R : Result> : Stat<R> {
    /**
     * Updates the statistic with a new value.
     *
     * @param value The observation to add.
     * @param weight The importance of this observation (default 1.0).
     */
    fun update(value: Double, weight: Double = 1.0)
}

/**
 * Statistics derived from pairs of numerical values, measuring their interaction.
 *
 * **Covers:** Correlation (Pearson, Spearman), Covariance, Simple Linear Regression.
 */
interface PairedStat<R : Result> : Stat<R> {
    /**
     * Updates the statistic with a pair of values.
     *
     * @param x The independent variable or first metric.
     * @param y The dependent variable or second metric.
     * @param weight The importance of this pair (default 1.0).
     */
    fun update(x: Double, y: Double, weight: Double = 1.0)
}

/**
 * Statistics that track the rate of events over time.
 * **Covers:** Throughput, Latency Decay, Windowed Rates.
 *
 * Base unit: **Nanoseconds**.
 */
interface TimeStat<R : Result> : Stat<R> {
    /**
     * Updates with an explicit timestamp in Nanoseconds.
     */
    fun update(value: Double, nanos: Long, weight: Double=1.0)

    /**
     * Updates using a Duration (e.g. relative to start).
     */
    fun update(value: Double, timestamp: Duration, weight: Double=1.0) =
        update(value, timestamp.inWholeNanoseconds, weight)
}

fun <R : Result> TimeStat<R>.atNow(): SeriesStat<R> = object : SeriesStat<R> {
    override fun update(value: Double, weight: Double) =
        this@atNow.update(value, System.nanoTime(), weight)

    override fun merge(values: R) = this@atNow.merge(values)
    override fun reset() = this@atNow.reset()
    override val name: String? get() = this@atNow.name
    override fun read(): R = this@atNow.read()
    override val mode: StreamMode get() = this@atNow.mode
}

/**
 * Statistics derived from multidimensional vectors.
 *
 * **Covers:** Centroids, Multivariate Normal Distribution, PCA (Covariance Matrix).
 */
interface VectorStat<R : Result> : Stat<R> {
    /**
     * Updates the statistic with a vector.
     *
     * @param vector The data point array.
     * @param weight The importance of this vector (default 1.0).
     */
    fun update(vector: DoubleArray, weight: Double = 1.0)
}
