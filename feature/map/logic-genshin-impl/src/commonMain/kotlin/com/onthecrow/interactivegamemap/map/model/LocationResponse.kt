package com.onthecrow.interactivegamemap.map.model

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.element
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.double
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive

@Serializable
data class LocationResponse(
    val data: List<Location>,
)

object LocationSerializer : KSerializer<Location> {
    override val descriptor: SerialDescriptor =
        buildClassSerialDescriptor("Location") {
            element<Int>("id")
            element<String>("type")
            element<Int>("mapId")
            element<Int>("level")
            element<Double>("lng")
            element<Double>("lat")
        }

    override fun deserialize(decoder: Decoder): Location {
        val jsonDecoder = decoder as? JsonDecoder
            ?: throw SerializationException("LocationSerializer works only with JSON")
        val arr = jsonDecoder.decodeJsonElement().jsonArray

        fun req(i: Int): JsonElement =
            arr.getOrNull(i) ?: throw SerializationException("Expected 6 elements, missing index $i: $arr")

        val id = req(0).jsonPrimitive.int
        val type = req(1).jsonPrimitive.content
        val mapId = req(2).jsonPrimitive.int
        val level = req(3).jsonPrimitive.int
        val lng = req(4).jsonPrimitive.double
        val lat = req(5).jsonPrimitive.double

        return Location(id, type, mapId, level, lat, lng)
    }

    override fun serialize(encoder: Encoder, value: Location) {
        val jsonEncoder = encoder as? JsonEncoder
            ?: throw SerializationException("DataRowSerializer works only with JSON")
        val arr = buildJsonArray {
            add(value.id)
            add(value.type)
            add(value.mapId)
            add(value.level)
            add(value.lng)
            add(value.lat)
        }
        jsonEncoder.encodeJsonElement(arr)
    }
}
