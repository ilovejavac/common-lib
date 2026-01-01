package com.dev.lib.ai.model

data class AceItem(
    val id: String,
    var section: String,
    var content: String,
    var helpful: Int = 0,
    var harmful: Int = 0,
    var neutral: Int = 0
) {
}