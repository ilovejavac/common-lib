package com.dev.lib.rabbit.poller

data class RabbitRetryPayload(
    val destination: String,
    val body: Any?,
    val routingKey: String = "",
    val headers: Map<String, String> = emptyMap(),
    val persistent: Boolean = true
)
