package com.eignex.katom.stat

import com.eignex.katom.core.*

class ExpandedVectorStat<R : Result>(
    val dimensions: Int,
    factory: (index: Int) -> SeriesStat<R>,
    override val name: String? = null,
) : VectorStat<ResultList<R>> {

    private val stats: Array<SeriesStat<R>> =
        Array(dimensions) { i -> factory(i) }

    override fun update(
        vector: DoubleArray,
        timestampNanos: Long,
        weight: Double
    ) {
        require(vector.size == dimensions) {
            "Vector size ${vector.size} does not match expected dimensions $dimensions"
        }

        for (i in 0 until dimensions) {
            stats[i].update(vector[i], timestampNanos, weight)
        }
    }

    override fun read(timestampNanos: Long): ResultList<R> {
        return ResultList(stats.map { it.read(timestampNanos) }, name)
    }

    override fun merge(values: ResultList<R>) {
        require(values.results.size == dimensions)
        for (i in 0 until dimensions) {
            @Suppress("UNCHECKED_CAST")
            stats[i].merge(values.results[i] as R)
        }
    }

    override fun reset() {
        for (stat in stats) stat.reset()
    }
}
