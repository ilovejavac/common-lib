package com.dev.lib.kafka

data class KafkaRetryPayload(
    val destination: String,
    val key: String = "",
    val body: Any?,
    val headers: Map<String, String> = emptyMap()
)
