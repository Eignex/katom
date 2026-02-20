package com.eignex.katom.core

import com.eignex.katom.stat.ExpandedVectorStat

// Transforms a Vector (ND) down to a Series (1D)
inline fun <R : Result> SeriesStat<R>.mapFromVector(
    crossinline transform: (vector: DoubleArray) -> Double
): VectorStat<R> = object : VectorStat<R>, Stat<R> by this {
    override fun update(vector: DoubleArray, timestampNanos: Long, weight: Double) {
        this@mapFromVector.update(transform(vector), timestampNanos, weight)
    }
}

// Transforms a Pair (2D) down to a Series (1D)
inline fun <R : Result> SeriesStat<R>.mapFromPaired(
    crossinline transform: (x: Double, y: Double) -> Double
): PairedStat<R> = object : PairedStat<R>, Stat<R> by this {
    override fun update(x: Double, y: Double, timestampNanos: Long, weight: Double) {
        this@mapFromPaired.update(transform(x, y), timestampNanos, weight)
    }
}

// Transforms a Series (1D) into another Series (1D)
inline fun <R : Result> SeriesStat<R>.mapSeries(
    crossinline transform: (value: Double) -> Double
): SeriesStat<R> = object : SeriesStat<R>, Stat<R> by this {
    override fun update(value: Double, timestampNanos: Long, weight: Double) {
        this@mapSeries.update(transform(value), timestampNanos, weight)
    }
}

fun <R : Result> SeriesStat<R>.onX(): PairedStat<R> =
    object : PairedStat<R>, Stat<R> by this {
        override fun update(
            x: Double, y: Double, timestampNanos: Long, weight: Double
        ) {
            this@onX.update(x, timestampNanos, weight)
        }
    }

fun <R : Result> SeriesStat<R>.onY(): PairedStat<R> =
    object : PairedStat<R>, Stat<R> by this {
        override fun update(
            x: Double, y: Double, timestampNanos: Long, weight: Double
        ) {
            this@onY.update(y, timestampNanos, weight)
        }
    }

fun <R : Result> SeriesStat<R>.atIndex(index: Int): VectorStat<R> =
    object : VectorStat<R>, Stat<R> by this {
        override fun update(
            vector: DoubleArray, timestampNanos: Long, weight: Double
        ) {
            this@atIndex.update(vector[index], timestampNanos, weight)
        }
    }

fun <R : Result> PairedStat<R>.atIndices(
    indexX: Int, indexY: Int
): VectorStat<R> = object : VectorStat<R>, Stat<R> by this {
    override fun update(
        vector: DoubleArray, timestampNanos: Long, weight: Double
    ) {
        this@atIndices.update(
            vector[indexX], vector[indexY], timestampNanos, weight
        )
    }
}

fun <R : Result> PairedStat<R>.withTimeAsX(): SeriesStat<R> =
    object : SeriesStat<R>, Stat<R> by this {
        override fun update(
            value: Double, timestampNanos: Long, weight: Double
        ) {
            this@withTimeAsX.update(
                x = timestampNanos / 1e9, y = value, timestampNanos, weight
            )
        }
    }

fun <R : Result> PairedStat<R>.withTimeAsY(): SeriesStat<R> =
    object : SeriesStat<R>, Stat<R> by this {
        override fun update(
            value: Double, timestampNanos: Long, weight: Double
        ) {
            this@withTimeAsY.update(
                x = value, y = timestampNanos / 1e9, timestampNanos, weight
            )
        }
    }

fun <R : Result> PairedStat<R>.withFixedX(fixedX: Double): SeriesStat<R> =
    object : SeriesStat<R>, Stat<R> by this {
        override fun update(
            value: Double, timestampNanos: Long, weight: Double
        ) {
            this@withFixedX.update(
                x = fixedX, y = value, timestampNanos, weight
            )
        }
    }


fun <R : Result> PairedStat<R>.withFixedY(fixedY: Double): SeriesStat<R> =
    object : SeriesStat<R>, Stat<R> by this {
        override fun update(
            value: Double, timestampNanos: Long, weight: Double
        ) {
            this@withFixedY.update(
                x = value, y = fixedY, timestampNanos, weight
            )
        }
    }

fun <R : Result> ((Int) -> SeriesStat<R>).expandedToVector(
    dimensions: Int
): VectorStat<ResultList<R>> {
    return ExpandedVectorStat(dimensions, this)
}

inline fun <R : Result> SeriesStat<R>.filter(
    crossinline predicate: (value: Double) -> Boolean
): SeriesStat<R> = object : SeriesStat<R>, Stat<R> by this {
    override fun update(value: Double, timestampNanos: Long, weight: Double) {
        if (predicate(value)) {
            this@filter.update(value, timestampNanos, weight)
        }
    }
}

inline fun <R : Result> PairedStat<R>.filter(
    crossinline predicate: (x: Double, y: Double) -> Boolean
): PairedStat<R> = object : PairedStat<R>, Stat<R> by this {
    override fun update(x: Double, y: Double, timestampNanos: Long, weight: Double) {
        if (predicate(x, y)) {
            this@filter.update(x, y, timestampNanos, weight)
        }
    }
}

inline fun <R : Result> VectorStat<R>.filter(
    crossinline predicate: (vector: DoubleArray) -> Boolean
): VectorStat<R> = object : VectorStat<R>, Stat<R> by this {
    override fun update(vector: DoubleArray, timestampNanos: Long, weight: Double) {
        if (predicate(vector)) {
            this@filter.update(vector, timestampNanos, weight)
        }
    }
}
