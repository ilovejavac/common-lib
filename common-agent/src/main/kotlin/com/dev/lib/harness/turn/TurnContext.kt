package com.dev.lib.harness.turn

import java.time.Instant

data class TurnContext(
    val currentData: Instant,

    val submission: String,
)