package com.dev.lib.harness.session

class Submission(
    val id: String,
    val op: Op
) {
}

sealed interface Op {
    data class UserInput(
        val text: String
    ) : Op

    object UserInterrupt : Op
}

