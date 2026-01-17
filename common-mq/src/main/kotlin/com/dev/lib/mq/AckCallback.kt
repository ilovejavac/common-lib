package com.dev.lib.mq

interface AckCallback<T> {

    fun onSuccess(message: MessageExtend<T>)

    fun onFailure(message: MessageExtend<T>, e: Throwable)

    companion object {
        @Suppress("UNCHECKED_CAST")
        fun <T> empty(): AckCallback<T> = Empty as AckCallback<T>

        private val Empty = object : AckCallback<Nothing> {
            override fun onSuccess(message: MessageExtend<Nothing>) {}
            override fun onFailure(message: MessageExtend<Nothing>, e: Throwable) {}
        }
    }
}
