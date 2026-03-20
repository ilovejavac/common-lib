package com.dev.lib.harness.session

import com.dev.lib.harness.protocol.Command

data class Submission(
    val id: String,
    val op: Command
)