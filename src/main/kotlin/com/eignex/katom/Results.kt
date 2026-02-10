package com.eignex.katom

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.math.sqrt
import kotlin.time.Duration
import kotlin.time.DurationUnit

@Serializable
sealed interface Result {
    val name: String
}

interface HasCount : Result {
    val count: Long
}

interface HasTotalWeights : Result {
    val totalWeights: Double
}

interface HasSum : Result {
    val sum: Double
}

interface HasRange : Result {
    val min: Double
    val max: Double
}

interface HasRate : Result {
    /** The normalized rate in Events Per Second (Hz) */
    val rate: Double
    val timestampNanos: Long

    /**
     * Rescales the throughput to a specific time duration.
     * Example: rate.per(1.minutes) returns Events Per Minute.
     */
    fun per(duration: Duration): Double = rate * duration.toDouble(
        DurationUnit.SECONDS
    )
}

interface HasEntropy : Result {
    val entropy: Double
}

interface HasMean : Result {
    val mean: Double
}

interface HasVariance : Result {
    val variance: Double

    val stdDev: Double get() = sqrt(variance)
}

interface HasUnbiasedVariance : HasTotalWeights, HasVariance {

    /**
     * Derived Unbiased Sample Variance using Bessel's correction.
     * Calculated as: variance * (n / (n - 1))
     */
    val unbiasedVariance: Double
        get() = if (totalWeights > 0.0) {
            variance * (totalWeights / (totalWeights - 1.0))
        } else {
            0.0
        }

    val unbiasedStdDev: Double get() = sqrt(unbiasedVariance)
}

interface HasShapeMoments : HasTotalWeights {
    val skew: Double
    val kurtosis: Double

    val unbiasedSkew: Double
        get() {
            if (totalWeights <= 2 || skew == 0.0) return 0.0
            return (sqrt(totalWeights * (totalWeights - 1)) / (totalWeights - 2)) * skew
        }

    val unbiasedKurtosis: Double get(){
        if (totalWeights <= 3) return 0.0
        val factor1 = (totalWeights - 1) / ((totalWeights - 2) * (totalWeights - 3))
        val factor2 = (totalWeights + 1) * kurtosis + 6.0
        return factor1 * factor2
    }
}

interface HasQuantiles : Result {
    val probabilities: DoubleArray
    val quantiles: DoubleArray
}

interface HasHistogram : Result {
    val bounds: DoubleArray
    val counts: LongArray
}

interface HasCardinality : Result {
    val cardinality: Long
    val errorBounds: Double
}

interface HasVectorSum : Result {
    val sums: DoubleArray
}

interface HasVectorRange : Result {
    val mins: DoubleArray
    val maxs: DoubleArray
}

interface HasVectorMean : Result {
    val means: DoubleArray
}

interface HasVectorVar : Result {
    val variances: DoubleArray
}

interface HasVectorNorms : Result {
    val l1Norm: Double
    val l2Norm: Double
}

interface HasCovarianceMatrix : Result {
    val dimension: Int
    val matrix: DoubleArray
}

interface HasCovariance : Result {
    val covariance: Double
}

interface HasCorrelation : Result {
    val correlation: Double
}

interface HasRegression : HasTotalWeights {
    val intercept: Double

    /**
     * SSE: Sum of Squared Errors (Unexplained variance).
     */
    val sse: Double

    /**
     * SST: Total Sum of Squares (Total variance of y).
     */
    val sst: Double

    val rSquared: Double get() = if (sst > 0) 1.0 - (sse / sst) else 0.0
    val mse: Double get() = if (totalWeights > 0) sse / totalWeights else 0.0
    val rmse: Double get() = sqrt(mse)
}

interface HasSLR : HasRegression {
    val slope: Double
}

interface HasMLR : HasRegression {
    val coefficients: DoubleArray
}

interface HasClassification : HasTotalWeights {

    val tp: Double
    val fp: Double
    val tn: Double
    val fn: Double

    val accuracy: Double get() = (tp + tn) / (tp + tn + fp + fn)
    val precision: Double get() = if ((tp + fp) > 0) tp / (tp + fp) else 0.0
    val recall: Double get() = if ((tp + fn) > 0) tp / (tp + fn) else 0.0
    val f1Score: Double get() = if ((precision + recall) > 0) 2 * (precision * recall) / (precision + recall) else 0.0

    val matrix: Array<DoubleArray>
}

@Serializable
@SerialName("Count")
data class CountResult(
    override val name: String,
    override val count: Long
) : HasCount

@Serializable
@SerialName("Sum")
data class SumResult(
    override val name: String,
    override val sum: Double
) : HasSum

@Serializable
@SerialName("Mean")
data class MeanResult(
    override val name: String,
    override val mean: Double,
) : HasMean

@Serializable
@SerialName("WeightedMean")
data class WeightedMeanResult(
    override val name: String,
    override val totalWeights: Double,
    override val mean: Double,
) : HasTotalWeights, HasMean

@Serializable
@SerialName("VarianceResult")
data class VarianceResult(
    override val name: String,
    override val mean: Double,
    override val variance: Double,
) : HasMean, HasVariance

@Serializable
@SerialName("WeightedVariance")
data class WeightedVarianceResult(
    override val name: String,
    override val totalWeights: Double,
    override val mean: Double,
    override val variance: Double
) : HasTotalWeights, HasMean, HasVariance

@Serializable
@SerialName("Moments")
data class MomentsResult(
    override val name: String,
    override val totalWeights: Double,
    override val mean: Double,
    override val variance: Double,
    override val skew: Double,
    override val kurtosis: Double
) : HasTotalWeights, HasMean, HasVariance, HasShapeMoments

@Serializable
@SerialName("Range")
data class RangeResult(
    override val name: String,
    override val min: Double,
    override val max: Double
) : HasRange

@Serializable
@SerialName("Rate")
data class RateResult(
    override val name: String,
    override val rate: Double,
    override val timestampNanos: Long
) : HasRate

@Serializable
@SerialName("Quantiles")
data class QuantilesResult(
    override val name: String,
    override val probabilities: DoubleArray,
    override val quantiles: DoubleArray
) : HasQuantiles

@Serializable
@SerialName("Histogram")
data class HistogramResult(
    override val name: String,
    override val bounds: DoubleArray,
    override val counts: LongArray
) : HasHistogram

@Serializable
@SerialName("VectorRange")
data class VectorRangeResult(
    override val name: String,
    override val mins: DoubleArray,
    override val maxs: DoubleArray
) : HasVectorRange

@Serializable
@SerialName("VectorVariance")
data class VectorVarianceResult(
    override val name: String,
    override val totalWeights: Double,
    override val means: DoubleArray,
    override val variances: DoubleArray
) : HasTotalWeights, HasVectorMean, HasVectorVar

@Serializable
@SerialName("SLR")
data class SLRResult(
    override val name: String,
    override val totalWeights: Double,
    val xMean: Double,
    val xVariance: Double,
    val yMean: Double,
    val yVariance: Double,
    override val covariance: Double
) :
    HasTotalWeights,
    HasCovariance,
    HasCorrelation,
    HasSLR, Result {

    // --- Correlation & Regression Basics ---

    override val correlation: Double
        get() {
            val sx = sqrt(xVariance)
            val sy = sqrt(yVariance)
            return if (sx > 0 && sy > 0) covariance / (sx * sy) else 0.0
        }

    override val slope: Double
        get() = if (xVariance > 0) covariance / xVariance else 0.0

    override val intercept: Double
        get() = yMean - (slope * xMean)

    override val sse: Double
        get() {
            if (totalWeights < 2) return 0.0
            val totalSumSquaresY = yVariance * (totalWeights - 1)
            return totalSumSquaresY * (1.0 - rSquared)
        }
    override val sst: Double
        get() = TODO("Not yet implemented")

    override val rmse: Double
        get() = sqrt(mse)
}
