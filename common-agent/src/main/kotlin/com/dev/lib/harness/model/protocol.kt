package com.dev.lib.harness.model


data class UserInput(
    val text: String,
)

enum class ReviewDecision {
    Approved,
    Denied,
    Abort
}