package com.dev.lib.ai.model

import com.dev.lib.exceptions.BizException

data class AiAgentException(
    val err: AiAgentErrorCode
) : BizException(err.code, err.message)