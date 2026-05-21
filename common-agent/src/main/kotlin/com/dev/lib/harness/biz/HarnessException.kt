package com.dev.lib.harness.biz

import com.dev.lib.exceptions.BizException

class HarnessException(error: HarnessError) : BizException(error)