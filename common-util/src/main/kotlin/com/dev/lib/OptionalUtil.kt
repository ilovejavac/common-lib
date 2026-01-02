package com.dev.lib

import java.util.Optional

fun <T> Optional<T>.orNull(): T? = orElse(null)
