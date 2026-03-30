package com.dev.libtest

import com.dev.lib.CoroutineScopeHolder
import com.dev.lib.None
import com.dev.lib.harness.protocol.Prompt
import com.dev.lib.harness.protocol.QueryOptions
import com.dev.lib.harness.protocol.RequestItem
import com.dev.lib.harness.protocol.TurnContext
import com.dev.lib.harness.sdk.SaaAgent
import com.dev.lib.harness.sdk.model.LlmClient
import com.dev.lib.harness.sdk.model.ModelProvider
import com.dev.lib.harness.sdk.model.ModelStorage
import kotlinx.coroutines.*
import org.springframework.ai.anthropic.AnthropicChatModel
import org.springframework.ai.anthropic.AnthropicChatOptions
import org.springframework.ai.anthropic.api.AnthropicApi
import org.springframework.ai.chat.messages.MessageType
import org.springframework.ai.chat.model.ChatModel
import org.springframework.ai.openai.OpenAiChatModel
import org.springframework.ai.openai.OpenAiChatOptions
import org.springframework.ai.openai.api.OpenAiApi
import org.springframework.ai.tool.annotation.Tool
import org.springframework.ai.tool.annotation.ToolParam


class Tools {

    @Tool(description = "查看指定城市天气")
    fun getWeather(city: String): String {
        return "$city 接下来 7 天一直是晴天"
    }

    @Tool(description = "读取文件内容")
    fun read(@ToolParam(description = "文件路径") path: String): String {
        return "def calculate_average(numbers):\n" + "    if not numbers:\n" + "        return 0\n" + "    total = 0\n" + "    for num in numbers:\n" + "        total += num\n" + "    return total / len(numbers)\n" + "\n" + "\n" + "def get_user_name(user):\n" + "    return user[\"name\"].upper()"
    }

    @Tool(description = "把内容写到指定文件中")
    fun write(@ToolParam(description = "文件路径") path: String, @ToolParam(description = "文件内容") content: String) {
        println("done")
    }
}

fun main(args: Array<String>) {
// 创建模型实例
    val globalScope = CoroutineScope(Dispatchers.IO + SupervisorJob() + CoroutineName("app-global-scope"))

    CoroutineScopeHolder.initGlobalScope(globalScope)


    val api = OpenAiApi.builder()
        .baseUrl("https://open.bigmodel.cn/api/coding/paas")
        .apiKey("9700fb274c4d4b3a94ac6de93d005fc3.2QlJUKLnb5QWR6sX")
        .completionsPath("/v4/chat/completions")
        .build()
    val chatModel: ChatModel = OpenAiChatModel.builder()
        .openAiApi(api)
        .defaultOptions(
            OpenAiChatOptions.builder()
                .model("glm-5")
                .maxTokens(131072)
                .temperature(0.2)
                .reasoningEffort("")
                .build()
        )
        .build()
//
//    val api = AnthropicApi.builder()
//        .baseUrl("https://open.bigmodel.cn/api/anthropic")
//        .apiKey("9700fb274c4d4b3a94ac6de93d005fc3.2QlJUKLnb5QWR6sX")
//        .build()
//
//    val chatModel = AnthropicChatModel.builder()
//        .anthropicApi(api)
//        .defaultOptions(AnthropicChatOptions.builder()
//            .model("glm-5")
//            .maxTokens(128 * 1024)
//            .temperature(1.0)
//            .thinking(AnthropicApi.ThinkingType.ENABLED, 2048)
//            .build())
//        .build();

    val agent = SaaAgent()

    val job = agent.query(
        prompt = Prompt(
            input = listOf(
                RequestItem.Message(
                    id = "",
                    role = MessageType.USER,
                    content = "我想去杭州玩",
                    endTurn = false,
                    phase = None
                )
            ), parallelToolCalls = true, tools = listOf(Tools())
        ), options = QueryOptions(
            turnContext = TurnContext("1", "gpt-5.4"), modelProvider = ModelProvider(object : ModelStorage {
                override fun getModel(id: String): LlmClient {
                    return object : LlmClient {
                        override fun getChatModel(): ChatModel {
                            return chatModel
                        }
                    }
                }
            })
        )
    )

    runBlocking {
        delay(1000 * 200)
    }
}