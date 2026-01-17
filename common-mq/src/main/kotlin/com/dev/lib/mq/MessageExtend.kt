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

    operator fun set(key: String, value: String) {
        _headers[key] = value
    }

    operator fun get(key: String): String? = _headers[key]

    fun persistent(persistent: Boolean): MessageExtend<T> = apply { this.persistent = persistent }

    fun ttl(millis: Long): MessageExtend<T> = apply { this.ttl = millis }

    fun delay(millis: Long): MessageExtend<T> = apply { this.delay = millis }

    fun priority(level: Int): MessageExtend<T> = apply { this.priority = level }

    fun sharding(key: String): MessageExtend<T> = apply { this.shardingKey = key }

    fun deadLetter(queue: String): MessageExtend<T> = apply { this.deadLetter = queue }

    fun retry(times: Int, delayMillis: Long = 1000): MessageExtend<T> = apply {
        this.retry = times
        this.retryDelay = delayMillis
    }

    companion object {
        fun <T> of(body: T): MessageExtend<T> = MessageExtend(body)
    }
}
