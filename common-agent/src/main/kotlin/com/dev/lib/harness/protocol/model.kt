package com.dev.lib.harness.protocol

import com.alibaba.cloud.ai.graph.NodeOutput
import com.alibaba.cloud.ai.graph.streaming.OutputType
import com.alibaba.cloud.ai.graph.streaming.StreamingOutput
import com.dev.lib.None
import com.dev.lib.Option
import com.dev.lib.Some
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.reactive.asFlow
import org.springframework.ai.chat.messages.AssistantMessage
import org.springframework.ai.chat.messages.ToolResponseMessage
import org.springframework.ai.chat.metadata.Usage
import reactor.core.publisher.Flux
import java.util.concurrent.ConcurrentHashMap

sealed interface ResponseEvent {

    val seq: Int

    data object Created : ResponseEvent {
        override val seq: Int
            get() = 0
    }

    data class OutputTextDelta(
        val itemId: String,
        val delta: String,
        override val seq: Int
    ) : ResponseEvent

    data class ReasoningContentDelta(
        val itemId: String,
        val delta: String,
        override val seq: Int
    ) : ResponseEvent

    data class ToolCallRequest(
        val callId: String,
        val name: String,
        val arguments: String,
        override val seq: Int
    ) : ResponseEvent

    data class ToolCallResult(
        val callId: String,
        val name: String,
        val result: String,
        override val seq: Int
    ) : ResponseEvent

    data class Completed(
        val lastAssistantMessage: Option<String>,
        val tokenUsage: Option<Usage>,
        override val seq: Int
    ) : ResponseEvent
}

class SaaResponseEventAdapter(
    val turnId: String,
    val flux: Flux<NodeOutput>
) {

    private var seq = 0
    private var createdSent = false
    private var completedSent = false
    private val pendingToolCalls = ConcurrentHashMap.newKeySet<String>()
    private var lastAssistantMessage: Option<String> = None
    private var usage: Option<Usage> = None

    suspend fun sampling(block: suspend (ResponseEvent) -> Unit) {
        flux.asFlow().filterIsInstance<StreamingOutput<*>>().collect {
            onOutput(it, block)
        }
    }

    private suspend fun onOutput(
        out: StreamingOutput<*>,
        emit: suspend (ResponseEvent) -> Unit
    ) {

        ensuerCreated(emit)

        when (out.outputType) {
            OutputType.AGENT_MODEL_STREAMING -> {
                val msg = out.message()

                if (msg is AssistantMessage) {
                    val (hasThinking, delta) = msg.resolveStreamingDelta() ?: return
                    emit(
                        if (hasThinking) {
                            ResponseEvent.ReasoningContentDelta(turnId, delta, seq++)
                        } else {
                            ResponseEvent.OutputTextDelta(turnId, delta, seq++)
                        }
                    )
                }
            }

            OutputType.AGENT_MODEL_FINISHED -> {
                val msg = out.message()
                if (msg is AssistantMessage) {
                    usage = Some(out.tokenUsage())
                    msg.text?.takeIf { it.isNotBlank() }.let {
                        lastAssistantMessage = Some(it!!)
                    }

                    if (msg.hasToolCalls()) {
                        msg.toolCalls.forEach { tc ->
                            pendingToolCalls += tc.id
                            emit(
                                ResponseEvent.ToolCallRequest(
                                    callId = tc.id,
                                    name = tc.name,
                                    arguments = tc.arguments,
                                    seq++
                                )
                            )
                        }
                    } else if (pendingToolCalls.isEmpty()) {
                        emitCompleted(emit)
                    }
                }
            }

            OutputType.AGENT_TOOL_FINISHED -> {
                val msg = out.message()
                if (msg is ToolResponseMessage) {
                    msg.responses.forEach { r ->
                        pendingToolCalls -= r.id
                        emit(
                            ResponseEvent.ToolCallResult(
                                callId = r.id,
                                name = r.name,
                                result = r.responseData,
                                seq++
                            )
                        )
                    }
                }
            }

            else -> Unit
        }
    }

    private suspend fun ensuerCreated(emit: suspend (ResponseEvent) -> Unit) {
        if (!createdSent) {
            createdSent = true
            seq++
//            emit(ResponseEvent.Created)
        }
    }

    private suspend fun emitCompleted(emit: suspend (ResponseEvent) -> Unit) {
        if (!completedSent) {
            completedSent = true
            emit(ResponseEvent.Completed(lastAssistantMessage, usage, seq++))
        }
    }

    private fun AssistantMessage.resolveStreamingDelta(): Pair<Boolean, String>? {
        val textDelta = text?.takeIf { it.isNotBlank() }
        val thoughtSignaturesDelta = metadata["thoughtSignatures"]?.toString()?.takeIf { it.isNotBlank() }
        val reasoningContentDelta = metadata["reasoningContent"]?.toString()?.takeIf { it.isNotBlank() }

        val thinkingDelta = when {
            metadata.containsKey("signature") && textDelta != null -> textDelta
            thoughtSignaturesDelta != null -> thoughtSignaturesDelta
            reasoningContentDelta != null -> reasoningContentDelta
            else -> null
        }

        return when {
            thinkingDelta != null -> true to thinkingDelta
            textDelta != null -> false to textDelta
            else -> null
        }
    }
}
