package com.dev.lib.mq

import com.dev.lib.entity.id.IDWorker
import java.io.Serializable
import java.time.LocalDateTime

data class MessageExtend<T>(
    val body: T
) : Serializable {

    var key: String = ""
    val id = IDWorker.newId()
    val producerAt = LocalDateTime.now()

    var persistent: Boolean = true
    var ttl: Long? = null
    var delay: Long? = null
    var priority: Int? = null
    var shardingKey: String? = null
    var deadLetter: String? = null
    var retry: Int = 3
    var retryDelay: Long = 1000

    private val _headers: MutableMap<String, String> = mutableMapOf()
    val headers: Map<String, String> get() = _headers

    operator fun set(headerKey: String, headerValue: String) {
        _headers[headerKey] = headerValue
    }

    operator fun get(headerKey: String): String? {
        return _headers[headerKey]
    }

    fun persistent(persistent: Boolean): MessageExtend<T> {
        this.persistent = persistent
        return this
    }

    fun ttl(millis: Long): MessageExtend<T> {
        this.ttl = millis
        return this
    }

    fun delay(millis: Long): MessageExtend<T> {
        this.delay = millis
        return this
    }

    fun priority(level: Int): MessageExtend<T> {
        this.priority = level
        return this
    }

    fun sharding(key: String): MessageExtend<T> {
        this.shardingKey = key
        return this
    }

    fun deadLetter(queue: String): MessageExtend<T> {
        this.deadLetter = queue
        return this
    }

    fun retry(times: Int, delayMillis: Long = 1000): MessageExtend<T> {
        this.retry = times
        this.retryDelay = delayMillis
        return this
    }

    companion object {
        fun <T> of(body: T): MessageExtend<T> {
            return MessageExtend(body)
        }
    }
}
