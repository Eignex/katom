package com.eignex.katom

class FrugalQuantile(
    val q: Double,
    val stepSize: Double = 0.01,
    val initialEstimate: Double = 0.0,
    override val mode: StreamMode = defaultStreamMode,
    override val name: String? = null
) : SeriesStat<QuantileResult>, HasQuantile {

    init {
        require(q in 0.0..1.0) { "Quantile q must be between 0.0 and 1.0" }
    }

    private val _estimate = mode.newDouble(initialEstimate)

    override val probability: Double get() = q
    override val quantile: Double by _estimate

    override fun update(value: Double, weight: Double) {
        if (weight <= 0.0) return

        val m = _estimate.load()
        val delta = if (value > m) {
            stepSize * q * weight
        } else if (value < m) {
            -stepSize * (1.0 - q) * weight
        } else {
            0.0
        }

        if (delta != 0.0) {
            _estimate.add(delta)
        }
    }

    override fun merge(values: QuantileResult) {
        val current = _estimate.load()
        _estimate.store((current + values.quantile) / 2.0)
    }

    override fun reset() {
        _estimate.store(initialEstimate)
    }

    override fun read() = QuantileResult(q, quantile, name)
}
