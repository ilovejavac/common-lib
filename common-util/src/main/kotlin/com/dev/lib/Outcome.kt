package com.dev.lib

sealed class Outcome<out T, out E> {

    fun isOk(): Boolean = this is Ok<*>

    fun isErr(): Boolean = !isOk()

    suspend fun then(block: suspend (T) -> Unit): Outcome<T, E> = apply {
        if (this is Ok<T>) {
            block(value)
        }
    }

    suspend fun match(
        error: @UnsafeVariance E, block: suspend () -> Unit
    ): Outcome<T, E> = apply {
        if (this is Err<*> && this.error == error) {
            block()
        }
    }
}

data class Ok<T>(val value: T) : Outcome<T, Nothing>()

data class Err<E>(val error: E) : Outcome<Nothing, E>()