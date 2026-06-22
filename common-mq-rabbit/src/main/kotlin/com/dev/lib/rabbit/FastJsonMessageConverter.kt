package com.dev.lib.rabbit

import com.dev.lib.util.Jsons
import org.springframework.amqp.core.Message
import org.springframework.amqp.core.MessageProperties
import org.springframework.amqp.support.converter.MessageConversionException
import org.springframework.amqp.support.converter.MessageConverter
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets

class FastJsonMessageConverter : MessageConverter {

    override fun toMessage(`object`: Any, messageProperties: MessageProperties): Message {
        return try {
            val outputStream = ByteArrayOutputStream()
            Jsons.writeWithType(outputStream, `object`)
            val body = outputStream.toByteArray()
            messageProperties.contentType = MessageProperties.CONTENT_TYPE_JSON
            messageProperties.contentEncoding = StandardCharsets.UTF_8.name()
            messageProperties.contentLength = body.size.toLong()
            Message(body, messageProperties)
        } catch (ex: Exception) {
            throw MessageConversionException("Failed to serialize Rabbit message with fastjson2", ex)
        }
    }

    override fun fromMessage(message: Message): Any {
        return try {
            Jsons.parseWithType(ByteArrayInputStream(message.body), Any::class.java)
        } catch (ex: Exception) {
            throw MessageConversionException("Failed to deserialize Rabbit message with fastjson2", ex)
        }
    }
}
