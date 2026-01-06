package com.dev.lib.ai.service.llm

import com.dev.lib.ai.model.ChatItem
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

data class AiModelLangchain(
    val model: String,
    val endpoint: ModelEndpoint,
    val baseUrl: String,
    val apiKey: String,
    val temperature: BigDecimal?,
    val topP: BigDecimal?,
    val maxTokens: Int
) : LLM {

    private val streamingClient: StreamingChatModel by lazy {
        when (endpoint) {
            ModelEndpoint.OPENAI -> OpenAiStreamingChatModel.builder()
                .baseUrl(ModelEndpoint.OPENAI.path_of(baseUrl))
                .apiKey(apiKey)
                .modelName(model)
                .temperature(temperature?.toDouble())
                .topP(topP?.toDouble())
                .maxTokens(maxTokens).build()

            ModelEndpoint.ANTHROPIC -> AnthropicStreamingChatModel.builder()
                .baseUrl(ModelEndpoint.ANTHROPIC.path_of(baseUrl))
                .apiKey(apiKey)
                .modelName(model)
                .temperature(temperature?.toDouble())
                .topP(topP?.toDouble())
                .maxTokens(maxTokens).build()
        }
    }

    private val syncClient: ChatModel by lazy {
        when (endpoint) {
            ModelEndpoint.OPENAI -> OpenAiChatModel.builder().baseUrl(baseUrl).apiKey(apiKey).modelName(model)
                .temperature(temperature?.toDouble()).topP(topP?.toDouble()).maxTokens(maxTokens).build()

            ModelEndpoint.ANTHROPIC -> AnthropicChatModel.builder().baseUrl(baseUrl).apiKey(apiKey).modelName(model)
                .temperature(temperature?.toDouble()).topP(topP?.toDouble()).maxTokens(maxTokens).build()
        }
    }

    override suspend fun stream(
        messages: List<ChatItem>, block: (chunk: String) -> Unit
    ): Response = suspendCancellableCoroutine<Response> { cont ->
        streamingClient.chat(messages.toLangchain(), object : StreamingChatResponseHandler {

            override fun onPartialResponse(partialResponse: String) {
                block(partialResponse)
            }

            override fun onCompleteResponse(completeResponse: ChatResponse) {
                cont.resumeWith(
                    Result.success(
                        Response(
                            thinking = completeResponse.aiMessage().thinking(),
                            content = completeResponse.aiMessage().text(),
                            inputTokenCount = completeResponse.tokenUsage()?.inputTokenCount(),
                            outputTokenCount = completeResponse.tokenUsage()?.outputTokenCount(),
                            totalTokenCount = completeResponse.tokenUsage()?.totalTokenCount()
                        )
                    )
                )
            }

            override fun onError(error: Throwable) {
                cont.resumeWithException(error)
            }
        })
    }

    override fun call(messages: List<ChatItem>): String {
        return syncClient.chat(messages.toLangchain()).aiMessage().text()
    }

    private fun List<ChatItem>.toLangchain() = map {
        when (it.role) {
            ChatRole.USER -> UserMessage.from(it.content)
            ChatRole.ASSISTANT -> AiMessage.from(it.content)
            ChatRole.SYSTEM -> SystemMessage.from(it.content)
        }
    }
}
