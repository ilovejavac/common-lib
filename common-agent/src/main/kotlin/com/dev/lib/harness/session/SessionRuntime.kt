package com.dev.lib.harness.session

class SessionRuntime (
    val state: SessionState
) {
}

enum class SessionState {
    IDLE, RUNNING, CLOSED
}