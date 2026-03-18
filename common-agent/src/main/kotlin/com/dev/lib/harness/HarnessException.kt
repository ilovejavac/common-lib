package com.dev.lib.harness

import com.dev.lib.exceptions.BizException
import com.dev.lib.web.model.CodeEnums

class HarnessException(
    error: HarnessError
) : BizException(
    error.code, error.message
)

enum class HarnessError(
    val code: Int,
    private val text: String
) : CodeEnums {
    QUEUE_IS_FULL(601001, "队列已满");

    override fun getCode() = code
    override fun getMessage() = text
}
