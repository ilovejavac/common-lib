package com.dev.lib.harness.biz

import com.dev.lib.web.model.CodeEnums

enum class HermessError(
    val code: Int,
    val message: String
) : CodeEnums {
    ;

    override fun getCode(): Int = code

    override fun getMessage(): String = message
}