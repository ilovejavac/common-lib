package com.dev.libtest

import com.dev.lib.CoroutineScopeHolder
import com.dev.lib.harness.protocol.RequestInput
import com.dev.lib.harness.sdk.Agent
import com.dev.lib.harness.sdk.model.LlmClient
import com.dev.lib.harness.sdk.model.ModelProvider
import com.dev.lib.harness.sdk.model.ModelStorage
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.springframework.ai.anthropic.AnthropicChatModel
import org.springframework.ai.anthropic.AnthropicChatOptions
import org.springframework.ai.anthropic.api.AnthropicApi
import org.springframework.ai.chat.model.ChatModel
import org.springframework.ai.tool.annotation.Tool
import org.springframework.ai.tool.annotation.ToolParam


class Tools {

    @Tool(description = "查看指定城市天气")
    fun getWeather(city: String): String {
        return "$city 接下来 7 天一直是晴天"
    }

    @Tool(description = "读取文件内容")
    fun read(@ToolParam(description = "文件路径") path: String): String {
        return "def calculate_average(numbers):\n" +
                "    if not numbers:\n" +
                "        return 0\n" +
                "    total = 0\n" +
                "    for num in numbers:\n" +
                "        total += num\n" +
                "    return total / len(numbers)\n" +
                "\n" +
                "\n" +
                "def get_user_name(user):\n" +
                "    return user[\"name\"].upper()"
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


//    val api = OpenAiApi.builder()
//        .baseUrl("https://yunyi.rdzhvip.com/codex")
//        .apiKey("4YF935JE-N95N-ZN7T-HF80-ZNBVYZ3W1U8Y")
//        .build()
//    val chatModel: ChatModel = OpenAiChatModel.builder()
//        .openAiApi(api)
//        .defaultOptions(
//            OpenAiChatOptions.builder()
//                .model("gpt-5.3-codex")
//                .maxTokens(131072)
//                .temperature(0.2)
//                .build()
//        )
//        .build()

    val api = AnthropicApi.builder()
        .baseUrl("https://open.bigmodel.cn/api/anthropic")
        .apiKey("4f66f6da192048b8895d4087bacbaa33.y8pbU6k82OgHufx1")
        .build();

    val chatModel = AnthropicChatModel.builder()
        .anthropicApi(api)
        .defaultOptions(
            AnthropicChatOptions.builder()
                .model("glm-5")
                .maxTokens(131072)
                .temperature(0.2)
                .build()
        )
        .build();

    Agent(
        ModelProvider(
            object : ModelStorage {
                override fun getModel(id: String): LlmClient {
                    return object : LlmClient {
                        override fun getChatModel(): ChatModel {
                            return chatModel
                        }
                    }
                }
            }
        )).query(
        RequestInput(
            model = "xx",
            message = "Find and fix the bug in /home/utils.py",
            tools = listOf(Tools())
        )
    )
}