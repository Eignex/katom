package com.eignex.katom

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ArraySerializer
import kotlinx.serialization.builtins.DoubleArraySerializer
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.CompositeEncoder
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlin.reflect.full.memberProperties
import kotlin.reflect.full.primaryConstructor

@Serializable(with = MapResultSerializer::class)
data class MapResult(val metrics: Map<String, Any>)

object MapResultSerializer : KSerializer<MapResult> {

    override val descriptor: SerialDescriptor = MapSerializer(String.serializer(), String.serializer()).descriptor

    override fun serialize(encoder: Encoder, value: MapResult) {
        val composite = encoder.beginStructure(descriptor)
        var index = 0
        value.metrics.forEach { (key, bit) ->
            composite.encodeStringElement(descriptor, index++, key)
            serializeValue(composite, index++, bit)
        }
        composite.endStructure(descriptor)
    }

    @OptIn(ExperimentalSerializationApi::class)
    private fun serializeValue(composite: CompositeEncoder, index: Int, value: Any) {
        when (value) {
            is Double -> composite.encodeDoubleElement(descriptor, index, value)
            is Long -> composite.encodeLongElement(descriptor, index, value)
            is Int -> composite.encodeIntElement(descriptor, index, value)
            is DoubleArray -> composite.encodeSerializableElement(descriptor, index, DoubleArraySerializer(), value)
            is Array<*> -> {
                @Suppress("UNCHECKED_CAST")
                composite.encodeSerializableElement(descriptor, index, ArraySerializer(DoubleArraySerializer()), value as Array<DoubleArray>)
            }
            else -> throw UnsupportedOperationException("$value")
        }
    }

    override fun deserialize(decoder: Decoder): MapResult =
        throw UnsupportedOperationException("Deserialization not supported for MapResult.")
}

fun Result.toMap(prefixResults: Boolean = true): MapResult =
    MapResult(this.flattenToMap(prefixResults))

private fun Result.flattenToMap(
    usePrefix: Boolean,
    prefix: String = ""
): Map<String, Any> {
    val constructorOrder = this::class.primaryConstructor?.parameters
        ?.mapIndexed { index, param -> param.name to index }
        ?.toMap() ?: emptyMap()

    val sortedProperties = this::class.memberProperties
        .filter { it.name in constructorOrder && it.name != "name" }
        .sortedBy { constructorOrder[it.name] }

    val destination = mutableMapOf<String, Any>()

    sortedProperties.forEach { prop ->
        val value = prop.call(this) ?: return@forEach
        val key = if (usePrefix) "$prefix${prop.name}" else prop.name

        if (value is Result) {
            val newPrefix = if (usePrefix) "$prefix${value.name}." else ""
            destination.putAll(value.flattenToMap(usePrefix, newPrefix))
        } else {
            destination[key] = value
        }
    }
    return destination
}
