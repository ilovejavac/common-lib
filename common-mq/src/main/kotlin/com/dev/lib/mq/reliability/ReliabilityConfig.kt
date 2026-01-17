package com.dev.lib.mq.reliability

data class ReliabilityConfig(
    val enableConfirm: Boolean = true,
    val enablePersistence: Boolean = true,
    val maxRetries: Int = 3,
    val retryInterval: Long = 1000,
    val enableDeadLetter: Boolean = true,
    val enableStorage: Boolean = false,
    val storage: MessageStorage? = null
) {

    companion object {
        val DEFAULT = ReliabilityConfig()
    }
}
