package com.eignex.katom

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.math.sqrt

@Serializable
sealed interface Result {
    val name: String?
}

@Serializable
@SerialName("Count")
data class CountResult(
    override val count: Long,
    override val name: String? = null
) : Result, HasCount

@Serializable
@SerialName("Sum")
data class SumResult(
    override val sum: Double,
    override val name: String? = null
) : Result, HasSum

@Serializable
@SerialName("Mean")
data class MeanResult(
    override val mean: Double,
    override val name: String? = null,
) : Result, HasMean

@Serializable
@SerialName("WeightedMean")
data class WeightedMeanResult(
    override val totalWeights: Double,
    override val mean: Double,
    override val name: String? = null,
) : Result, HasTotalWeights, HasMean


@Serializable
@SerialName("Variance")
data class VarianceResult(
    override val mean: Double,
    override val variance: Double,
    override val name: String? = null
) : Result, HasMean, HasVariance


@Serializable
@SerialName("WeightedVariance")
data class WeightedVarianceResult(
    override val totalWeights: Double,
    override val mean: Double,
    override val variance: Double,
    override val name: String? = null
) : Result, HasMean, HasSampleVariance

@Serializable
@SerialName("Moments")
data class MomentsResult(
    override val totalWeights: Double,
    override val mean: Double,
    override val m2: Double,
    override val m3: Double,
    override val m4: Double,
    override val name: String? = null
) : Result, HasTotalWeights, HasMean, HasSampleVariance, HasShapeMoments {
    override val sst: Double get() = m2
}

@Serializable
@SerialName("Range")
data class RangeResult(
    override val min: Double,
    override val max: Double,
    override val name: String? = null
) : Result, HasRange

@Serializable
@SerialName("Rate")
data class RateResult(
    val startTime: Long,
    val totalValue: Double,
    override val timestampNanos: Long,
    override val name: String? = null
) : Result, HasRate {
    override val rate: Double get(){
        val durationSeconds = (timestampNanos - startTime) / 1_000_000_000.0
        val safeDuration = if (durationSeconds <= 0.0) 1e-9 else durationSeconds
        return totalValue / safeDuration
    }
}

@Serializable
@SerialName("DecayingRate")
data class DecayingRateResult(
    override val rate: Double,
    override val timestampNanos: Long,
    override val name: String? = null
) : Result, HasRate

@Serializable
@SerialName("Quantile")
data class QuantileResult(
    override val probability: Double,
    override val quantile: Double,
    override val name: String? = null
) : Result, HasQuantile

@Serializable
@SerialName("Quantiles")
data class QuantilesResult(
    override val probabilities: DoubleArray,
    override val quantiles: DoubleArray,
    override val name: String? = null
) : Result, HasQuantiles

@Serializable
@SerialName("Histogram")
data class HistogramResult(
    override val bounds: DoubleArray,
    override val counts: LongArray,
    override val name: String? = null
) : Result, HasHistogram

@Serializable
@SerialName("VectorRange")
data class VectorRangeResult(
    override val mins: DoubleArray,
    override val maxs: DoubleArray,
    override val name: String? = null
) : Result, HasVectorRange

@Serializable
@SerialName("VectorVariance")
data class VectorVarianceResult(
    override val totalWeights: Double,
    override val means: DoubleArray,
    override val variances: DoubleArray,
    override val name: String? = null
) : Result, HasTotalWeights, HasVectorMean, HasVectorVar

@Serializable
@SerialName("OLS")
data class OLSResult(
    override val totalWeights: Double,
    override val slope: Double,
    override val intercept: Double,
    override val sse: Double,
    val x: VarianceResult,
    val y: VarianceResult,
    override val name: String? = null,
) : Result,
    HasLinearModel,
    HasRegression,
    HasCorrelation,
    HasCovariance {

    /**
     * Calculated from R² and the sign of the slope.
     * This avoids needing to store the raw covariance if not strictly necessary.
     */
    override val correlation: Double
        get() {
            if (sst <= 0.0) return 0.0
            val r2 = (1.0 - (sse / sst)).coerceAtLeast(0.0)
            val r = sqrt(r2)
            return if (slope >= 0) r else -r
        }
    override val sst: Double
        get() = y.variance * totalWeights

    override val covariance: Double
        get() = slope * x.variance
}
