package com.eignex.katom

import kotlin.time.Duration

/**
 * The base interface for all statistical accumulators.
 *
 * @param R The type of the result object produced by this statistic.
 */
interface Stat<out R : Result> {
    val name: String
    /**
     * Computes and returns the current state of the statistic.
     */
    fun read(): R
}

/**
 * Statistics derived from a single series of numerical values.
 *
 * **Covers:** Distributions (Mean, Variance, Skewness), Quantiles (P50, P99), Min/Max.
 */
interface SeriesStat<out R : Result> : Stat<R> {
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
interface PairedStat<out R : Result> : Stat<R> {
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
interface TimeStat<out R : Result> : Stat<R> {
    /**
     * Updates with an explicit timestamp in Nanoseconds.
     */
    fun update(value: Double, timestampNanos: Long, weight: Double=1.0)

    /**
     * Updates using a Duration (e.g. relative to start).
     */
    fun update(value: Double, timestamp: Duration, weight: Double=1.0) =
        update(value, timestamp.inWholeNanoseconds, weight)
}

/**
 * Statistics derived from multidimensional vectors.
 *
 * **Covers:** Centroids, Multivariate Normal Distribution, PCA (Covariance Matrix).
 */
interface VectorStat<out R : Result> : Stat<R> {
    /**
     * Updates the statistic with a vector.
     *
     * @param vector The data point array.
     * @param weight The importance of this vector (default 1.0).
     */
    fun update(vector: DoubleArray, weight: Double = 1.0)
}

/**
 * Statistics for evaluating regression models where inputs are vectors.
 *
 * **Covers:** RMSE (Root Mean Squared Error), MAE, R-Squared, Residual Analysis.
 */
interface ResponseStat<out R : Result> : Stat<R> {
    /**
     * Updates the statistic with a feature vector and its target outcome.
     *
     * @param x The independent variable or first metric.
     * @param y The dependent variable or second metric.
     * @param weight The importance of this sample (default 1.0).
     */
    fun update(x: DoubleArray, y: Double, weight: Double = 1.0)
}
