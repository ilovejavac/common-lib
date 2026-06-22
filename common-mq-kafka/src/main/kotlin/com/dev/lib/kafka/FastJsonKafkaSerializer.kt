package com.dev.lib.kafka

import com.dev.lib.util.Jsons
import org.apache.kafka.common.serialization.Deserializer
import org.apache.kafka.common.serialization.Serializer
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

class FastJsonKafkaSerializer : Serializer<Any> {

    override fun serialize(topic: String?, data: Any?): ByteArray? {
        if (data == null) {
            return null
        }
        val outputStream = ByteArrayOutputStream()
        Jsons.writeWithType(outputStream, data)
        return outputStream.toByteArray()
    }
}

class FastJsonKafkaDeserializer : Deserializer<Any> {

    override fun deserialize(topic: String?, data: ByteArray?): Any? {
        if (data == null || data.isEmpty()) {
            return null
        }
        return Jsons.parseWithType(ByteArrayInputStream(data), Any::class.java)
    }
}
