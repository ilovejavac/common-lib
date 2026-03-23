package com.dev.lib.harness.sdk.skill

import com.dev.lib.None
import com.dev.lib.Ok
import com.dev.lib.Some
import com.dev.lib.util.Jsons

data class SkillMeta(
    val name: String,
    val description: String,

    val pwd: String
)

fun main() {
    print(Jsons.toJson(None))
    print(Jsons.toJson(Some("hello")))
    print(Jsons.toJson(Ok("hello").isErr()))
}