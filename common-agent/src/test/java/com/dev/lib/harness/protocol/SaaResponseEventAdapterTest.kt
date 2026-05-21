package com.dev.lib.harness.protocol

import com.dev.lib.exceptions.BizException
import com.dev.lib.harness.biz.HarnessError
import com.dev.lib.harness.biz.HarnessException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class SaaResponseEventAdapterTest {

    @Test
    fun `harness exception keeps code and message from enum`() {
        val error = HarnessError.UNKNOWN

        val ex = assertThrows(BizException::class.java) {
            throw HarnessException(error)
        }

        assertEquals(500000, ex.coder)
        assertEquals("unknown harness error", ex.msger)
    }

}
