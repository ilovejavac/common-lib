package com.dev.lib.rocketmq

data class RocketRetryPayload(
    val destination: String,
    val key: String = "",
    val body: Any?,
    val headers: Map<String, String> = emptyMap()
)
