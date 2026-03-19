package com.dev.lib.harness.session

import com.dev.lib.harness.protocol.command

data class Submission(
    val id: String,
    val op: command
)