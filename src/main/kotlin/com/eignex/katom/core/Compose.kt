package com.eignex.katom.core

import com.eignex.katom.concurrent.StreamMode
import kotlinx.serialization.*
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

@Serializable
@SerialName("ResultList")
data class ResultList(
    val results: List<Result>,
) : Result {
    override val name: String? get() = null
}


// working, annotate fields in ResultsX
class ResultDelegateSerializer<T : Result> : KSerializer<T> {

    private val delegate = Result.serializer()
    override val descriptor = delegate.descriptor

    override fun serialize(encoder: Encoder, value: T) {
        encoder.encodeSerializableValue(delegate, value as Result)
    }

    @Suppress("UNCHECKED_CAST")
    override fun deserialize(decoder: Decoder): T {
        return decoder.decodeSerializableValue(delegate) as T
    }
}

@Serializable
@SerialName("Result2")
data class Result2<A : Result, B : Result>(
    @Serializable(with = ResultDelegateSerializer::class) val first: A,
    @Serializable(with = ResultDelegateSerializer::class) val second: B
) : Result {
    override val name: String? get() = null
}

@Serializable
@SerialName("Result3")
data class Result3<A : Result, B : Result, C : Result>(
    @Serializable(with = ResultDelegateSerializer::class) val first: A,
    @Serializable(with = ResultDelegateSerializer::class) val second: B,
    @Serializable(with = ResultDelegateSerializer::class) val third: C
) : Result {
    override val name: String? get() = null
}

@Serializable
@SerialName("Result4")
data class Result4<A : Result, B : Result, C : Result, D : Result>(
    @Serializable(with = ResultDelegateSerializer::class) val first: A,
    @Serializable(with = ResultDelegateSerializer::class) val second: B,
    @Serializable(with = ResultDelegateSerializer::class) val third: C,
    @Serializable(with = ResultDelegateSerializer::class) val fourth: D
) : Result {
    override val name: String? get() = null
}

class SeriesStat2<A : Result, B : Result>(
    val s1: SeriesStat<A>, val s2: SeriesStat<B>
) : SeriesStat<Result2<A, B>> {

    override fun update(value: Double, weight: Double) {
        s1.update(value, weight)
        s2.update(value, weight)
    }

    override fun read() = Result2<A, B>(s1.read(), s2.read())
    override val mode: StreamMode
        get() = TODO("Not yet implemented")

    override fun merge(values: Result2<A, B>) {
        s1.merge(values.first)
        s2.merge(values.second)
    }

    override fun reset() {
        TODO("Not yet implemented")
    }

    override val name: String = "seriesStat2"
}

class SeriesStat3<A : Result, B : Result, C : Result>(
    val s1: SeriesStat<A>,
    val s2: SeriesStat<B>,
    val s3: SeriesStat<C>,
) : SeriesStat<Result3<A, B, C>> {

    override fun update(value: Double, weight: Double) {
        s1.update(value, weight)
        s2.update(value, weight)
        s3.update(value, weight)
    }

    override fun read(): Result3<A, B, C> {
        val r1 = s1.read()
        val r2 = s2.read()
        val r3 = s3.read()
        return Result3(r1, r2, r3)
    }

    override val mode: StreamMode
        get() = TODO("Not yet implemented")

    override fun merge(values: Result3<A, B, C>) {
        s1.merge(values.first)
        s2.merge(values.second)
        s3.merge(values.third)
    }

    override fun reset() {
        TODO("Not yet implemented")
    }

    override val name: String = "seriesStat3"
}

class SeriesStat4<A : Result, B : Result, C : Result, D : Result>(
    val s1: SeriesStat<A>,
    val s2: SeriesStat<B>,
    val s3: SeriesStat<C>,
    val s4: SeriesStat<D>,
) : SeriesStat<Result4<A, B, C, D>> {

    override fun update(value: Double, weight: Double) {
        s1.update(value, weight)
        s2.update(value, weight)
        s3.update(value, weight)
        s4.update(value, weight)
    }

    override fun read(): Result4<A, B, C, D> {
        val r1 = s1.read()
        val r2 = s2.read()
        val r3 = s3.read()
        val r4 = s4.read()
        return Result4(r1, r2, r3, r4)
    }

    override val mode: StreamMode
        get() = TODO("Not yet implemented")

    override fun merge(values: Result4<A, B, C, D>) {
        s1.merge(values.first)
        s2.merge(values.second)
        s3.merge(values.third)
        s4.merge(values.fourth)
    }

    override fun reset() {
        TODO("Not yet implemented")
    }

    override val name: String = "seriesStat4"
}

class TimeStat2<A : Result, B : Result>(
    val s1: TimeStat<A>, val s2: TimeStat<B>
) : TimeStat<Result2<A, B>> {

    override fun update(value: Double, nanos: Long, weight: Double) {
        s1.update(value, nanos, weight)
        s2.update(value, nanos, weight)
    }

    override fun read() = Result2<A, B>(s1.read(), s2.read())
    override val mode: StreamMode
        get() = TODO("Not yet implemented")

    override fun merge(values: Result2<A, B>) {
        s1.merge(values.first)
        s2.merge(values.second)
    }

    override fun reset() {
        TODO("Not yet implemented")
    }

    override val name: String = "timeStat2"
}

class TimeStat3<A : Result, B : Result, C : Result>(
    val s1: TimeStat<A>,
    val s2: TimeStat<B>,
    val s3: TimeStat<C>,
) : TimeStat<Result3<A, B, C>> {

    override fun update(value: Double, nanos: Long, weight: Double) {
        s1.update(value, nanos, weight)
        s2.update(value, nanos, weight)
        s3.update(value, nanos, weight)
    }

    override fun read(): Result3<A, B, C> {
        val r1 = s1.read()
        val r2 = s2.read()
        val r3 = s3.read()
        return Result3(r1, r2, r3)
    }

    override val mode: StreamMode
        get() = TODO("Not yet implemented")

    override fun merge(values: Result3<A, B, C>) {
        s1.merge(values.first)
        s2.merge(values.second)
        s3.merge(values.third)
    }

    override fun reset() {
        TODO("Not yet implemented")
    }

    override val name: String = "timeStat3"
}

class TimeStat4<A : Result, B : Result, C : Result, D : Result>(
    val s1: TimeStat<A>,
    val s2: TimeStat<B>,
    val s3: TimeStat<C>,
    val s4: TimeStat<D>,
) : TimeStat<Result4<A, B, C, D>> {

    override fun update(value: Double, nanos: Long, weight: Double) {
        s1.update(value, nanos, weight)
        s2.update(value, nanos, weight)
        s3.update(value, nanos, weight)
        s4.update(value, nanos, weight)
    }

    override fun read(): Result4<A, B, C, D> {
        val r1 = s1.read()
        val r2 = s2.read()
        val r3 = s3.read()
        val r4 = s4.read()
        return Result4(r1, r2, r3, r4)
    }

    override val mode: StreamMode
        get() = TODO("Not yet implemented")

    override fun merge(values: Result4<A, B, C, D>) {
        s1.merge(values.first)
        s2.merge(values.second)
        s3.merge(values.third)
        s4.merge(values.fourth)
    }

    override fun reset() {
        TODO("Not yet implemented")
    }

    override val name: String = "timeStat4"
}


operator fun <A : Result, B : Result> SeriesStat<A>.plus(
    other: SeriesStat<B>
) = SeriesStat2(this, other)

operator fun <A : Result, B : Result, C : Result> SeriesStat2<A, B>.plus(
    other: SeriesStat<C>
) = SeriesStat3(s1, s2, other)

operator fun <A : Result, B : Result, C : Result> SeriesStat<A>.plus(
    other: SeriesStat2<B, C>
) = SeriesStat3(this, other.s1, other.s2)

operator fun <A : Result, B : Result, C : Result, D : Result> SeriesStat3<A, B, C>.plus(
    other: SeriesStat<D>
) = SeriesStat4(s1, s2, s3, other)

operator fun <A : Result, B : Result, C : Result, D : Result> SeriesStat<A>.plus(
    other: SeriesStat3<B, C, D>
) = SeriesStat4(this, other.s1, other.s2, other.s3)

operator fun <A : Result, B : Result> TimeStat<A>.plus(
    other: TimeStat<B>
) = TimeStat2(this, other)

operator fun <A : Result, B : Result, C : Result> TimeStat2<A, B>.plus(
    other: TimeStat<C>
) = TimeStat3(s1, s2, other)

operator fun <A : Result, B : Result, C : Result> TimeStat<A>.plus(
    other: TimeStat2<B, C>
) = TimeStat3(this, other.s1, other.s2)

operator fun <A : Result, B : Result, C : Result, D : Result> TimeStat3<A, B, C>.plus(
    other: TimeStat<D>
) = TimeStat4(s1, s2, s3, other)

operator fun <A : Result, B : Result, C : Result, D : Result> TimeStat<A>.plus(
    other: TimeStat3<B, C, D>
) = TimeStat4(this, other.s1, other.s2, other.s3)
