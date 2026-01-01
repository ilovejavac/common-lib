package com.dev.lib.ai.service.llm

import com.dev.lib.ai.model.ChatMessage
import com.dev.lib.ai.model.ChatRole
import com.dev.lib.ai.model.ModelEndpoint
import dev.langchain4j.data.message.AiMessage
import dev.langchain4j.data.message.SystemMessage
import dev.langchain4j.data.message.UserMessage
import dev.langchain4j.model.anthropic.AnthropicChatModel
import dev.langchain4j.model.anthropic.AnthropicStreamingChatModel
import dev.langchain4j.model.chat.ChatModel
import dev.langchain4j.model.chat.StreamingChatModel
import dev.langchain4j.model.chat.response.ChatResponse
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler
import dev.langchain4j.model.openai.OpenAiChatModel
import dev.langchain4j.model.openai.OpenAiStreamingChatModel
import kotlinx.coroutines.suspendCancellableCoroutine
import java.math.BigDecimal
import kotlin.coroutines.resumeWithException

typealias Response = com.dev.lib.ai.model.ChatResponse

class LangchainModel(
    private val model: String,
    private val endpoint: ModelEndpoint,
    private val baseUrl: String,  // 用户自定义 baseUrl，或用默认值
    private val apiKey: String,
    private val temperature: BigDecimal?,
    private val topP: BigDecimal?,
    private val maxTokens: Int?
) : LLM {

    private val streamingClient: StreamingChatModel by lazy {
        when (endpoint) {
            ModelEndpoint.OPENAI -> OpenAiStreamingChatModel.builder().baseUrl(baseUrl).apiKey(apiKey).modelName(model)
                .temperature(temperature?.toDouble()).topP(topP?.toDouble()).maxTokens(maxTokens).build()

            ModelEndpoint.ANTHROPIC -> AnthropicStreamingChatModel.builder().baseUrl(baseUrl).apiKey(apiKey)
                .modelName(model).temperature(temperature?.toDouble()).topP(topP?.toDouble())
                .maxTokens(maxTokens ?: 4096).build()
        }
    }

    private val syncClient: ChatModel by lazy {
        when (endpoint) {
            ModelEndpoint.OPENAI -> OpenAiChatModel.builder().baseUrl(baseUrl).apiKey(apiKey).modelName(model)
                .temperature(temperature?.toDouble()).topP(topP?.toDouble()).maxTokens(maxTokens).build()

            ModelEndpoint.ANTHROPIC -> AnthropicChatModel.builder().baseUrl(baseUrl).apiKey(apiKey).modelName(model)
                .temperature(temperature?.toDouble()).topP(topP?.toDouble()).maxTokens(maxTokens ?: 4096).build()
        }
    }

    override suspend fun stream(
        messages: List<ChatMessage>, block: (chunk: String) -> Unit
    ): Response = suspendCancellableCoroutine<Response> { cont ->
        streamingClient.chat(
            messages.toLangchain(), object : StreamingChatResponseHandler {

                override fun onPartialResponse(partialResponse: String) {
                    block(partialResponse)
                }

                override fun onCompleteResponse(completeResponse: ChatResponse) {
                    cont.resumeWith(Result.success(
                        Response(
                            thinking = completeResponse.aiMessage().thinking(),
                            content = completeResponse.aiMessage().text(),
                            inputTokenCount = completeResponse.metadata().tokenUsage().inputTokenCount(),
                            outputTokenCount = completeResponse.metadata().tokenUsage().outputTokenCount(),
                            totalTokenCount = completeResponse.metadata().tokenUsage().totalTokenCount(),
                        )
                    ))
                }

                override fun onError(error: Throwable) {
                    cont.resumeWithException(error)
                }
            })
    }


    override fun call(messages: List<ChatMessage>): String {
        return syncClient.chat(messages.toLangchain()).aiMessage().text()
    }

    private fun List<ChatMessage>.toLangchain() = map {
        when (it.role) {
            ChatRole.USER -> UserMessage.from(it.content)
            ChatRole.ASSISTANT -> AiMessage.from(it.content)
            ChatRole.SYSTEM -> SystemMessage.from(it.content)
        }
    }
}
