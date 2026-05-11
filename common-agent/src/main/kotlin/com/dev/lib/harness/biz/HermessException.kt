package com.dev.lib.harness.biz

import com.dev.lib.exceptions.BizException

class HermessException(error: HermessError) : BizException(error)