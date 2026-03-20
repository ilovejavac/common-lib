package com.dev.lib

import java.util.function.Consumer
import kotlin.reflect.KClass

sealed class Outcome<out T, out E> {
    data class Success<T>(val value: T) : Outcome<T, Nothing>()
    data class Failure<E>(val error: E) : Outcome<Nothing, E>()

    companion object {
        @JvmStatic
        fun <T> success(value: T): Outcome<T, Nothing> = Success(value)

        @JvmStatic
        fun <E> failure(error: E): Outcome<Nothing, E> = Failure(error)
    }

    inline fun then(block: (T) -> Unit): Outcome<T, E> = apply {
        if (this is Success<T>) {
            block(value)
        }
    }

    inline fun <reified X : Any> match(
        noinline block: (X) -> Unit
    ): Outcome<T, E> = match(X::class, block)

    fun <X : Any> match(
        type: KClass<X>,
        block: (X) -> Unit
    ): Outcome<T, E> = match(type.java, Consumer { block(it) })

    fun <X : Any> match(
        type: Class<X>,
        block: Consumer<in X>
    ): Outcome<T, E> = apply {
        if (this is Failure<*> && type.isInstance(error)) {
            block.accept(type.cast(error))
        }
    }
}
