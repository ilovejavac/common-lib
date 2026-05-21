package com.dev.lib.harness.biz

import com.dev.lib.web.model.CodeEnums

enum class HarnessError(
    private val bizCode: Int,
    private val bizMessage: String
) : CodeEnums {
    UNKNOWN(500000, "unknown harness error");

    override fun getCode(): Int = bizCode

    override fun getMessage(): String = bizMessage
}
