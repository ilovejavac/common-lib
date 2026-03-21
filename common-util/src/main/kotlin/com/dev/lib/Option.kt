package com.dev.lib

sealed class Option<out T>

data object None : Option<Nothing>()

data class Some<T>(val value: T) : Option<T>()