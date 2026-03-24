package com.dev.lib

sealed class Option<out T : Any> {

}

data object None : Option<Nothing>()

data class Some<T : Any>(val value: T) : Option<T>()