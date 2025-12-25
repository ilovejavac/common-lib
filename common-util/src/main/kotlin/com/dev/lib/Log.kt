package com.dev.lib

import com.sun.org.slf4j.internal.Logger
import com.sun.org.slf4j.internal.LoggerFactory

val <T : Any> T.log: Logger
    get() = LoggerFactory.getLogger(this::class.java)
