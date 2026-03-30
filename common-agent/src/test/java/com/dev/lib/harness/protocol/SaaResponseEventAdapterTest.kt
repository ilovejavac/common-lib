package com.dev.lib.harness.protocol

import com.alibaba.cloud.ai.graph.OverAllState
import com.alibaba.cloud.ai.graph.streaming.OutputType
import com.alibaba.cloud.ai.graph.streaming.StreamingOutput
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.ai.chat.messages.AssistantMessage
import reactor.core.publisher.Flux

class SaaResponseEventAdapterTest {

    @Test
    fun `should prefer non blank reasoning content when signature exists without text`() {
        runBlocking {
            val message = AssistantMessage.builder()
                .content("")
                .properties(
                    mapOf(
                        "signature" to "sig-1",
                        "reasoningContent" to "first thinking chunk"
                    )
                )
                .build()

            val output = StreamingOutput<Any>(
                message,
                "model",
                "agent",
                OverAllState(),
                OutputType.AGENT_MODEL_STREAMING
            )

            val events = mutableListOf<ResponseEvent>()
            SaaResponseEventAdapter("turn-1", Flux.just(output)).sampling { events += it }

            assertThat(events).hasSize(1)
            val event = events.single()
            assertThat(event).isInstanceOf(ResponseEvent.ReasoningContentDelta::class.java)
            assertThat((event as ResponseEvent.ReasoningContentDelta).delta).isEqualTo("first thinking chunk")
        }
    }
}
