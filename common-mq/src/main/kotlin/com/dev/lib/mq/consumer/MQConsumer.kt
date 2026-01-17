package com.dev.lib.mq.consumer

import com.dev.lib.mq.AckAction

interface MQConsumer {

    fun <T> listen(destination: String, handler: (T) -> AckAction)

    fun <T> listen(
        destination: String,
        group: String = "",
        concurrency: Int = 1,
        handler: (T) -> AckAction
    )
}
