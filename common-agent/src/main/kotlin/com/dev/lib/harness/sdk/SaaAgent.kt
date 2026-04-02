package com.dev.lib.harness.sdk

import com.alibaba.cloud.ai.graph.agent.ReactAgent
import com.dev.lib.harness.protocol.ResponseEvent
import com.dev.lib.harness.protocol.SaaResponseEventAdapter
import com.dev.lib.harness.protocol.TurnContext
import com.dev.lib.harness.protocol.UiEvent
import org.springframework.ai.chat.messages.*

class SaaAgent : Agent {

    override suspend fun query(
        prompts: List<Prompt>,
        context: TurnContext
    ) {
        val (session) = context
        val (model, userCustomPrompt, thinking, temperature, topP, maxTokens) = context.options
        val chatModel = context.model.getChatModel()

        val reactAgent = ReactAgent.builder()
            .name("test.ai")
            .model(chatModel)
            .build()



        val messages = prompts.toMessages()
        val stream = reactAgent.stream(messages)

        val adapter = SaaResponseEventAdapter(context.submissionId, stream)
        adapter.sampling { event ->
            session.emit(event.toUiEvent(context.submissionId))
        }
    }

    private fun List<Prompt>.toMessages(): List<Message> {
        return this.mapNotNull { prompt ->
            when (prompt) {
                is Prompt.Message -> {
                    when (prompt.role) {
                        MessageType.USER -> UserMessage(prompt.content)
                        MessageType.ASSISTANT -> AssistantMessage(prompt.content)
                        MessageType.SYSTEM -> SystemMessage(prompt.content)
                        MessageType.TOOL -> null
                    }
                }
            }
        }
    }
}

fun ResponseEvent.toUiEvent(submissionId: String): UiEvent = when (this) {
    is ResponseEvent.OutputTextDelta -> UiEvent.AgentMessageDelta(submissionId, delta)
    is ResponseEvent.ReasoningContentDelta -> UiEvent.AgentThinkingDelta(submissionId, delta)
    is ResponseEvent.ToolCallRequest -> UiEvent.ToolCall(callId, name, arguments)
    is ResponseEvent.ToolCallResult -> UiEvent.ToolResult(callId, name, result)
    is ResponseEvent.Created -> UiEvent.TurnStarted(submissionId)
    is ResponseEvent.Completed -> UiEvent.TurnComplete(submissionId)
}
