package com.dev.lib.harness.sdk

import org.springframework.ai.chat.messages.SystemMessage
import org.springframework.ai.chat.messages.UserMessage
import org.springframework.ai.chat.model.ChatModel
import org.springframework.ai.chat.prompt.Prompt
import org.springframework.ai.openai.OpenAiChatModel
import org.springframework.ai.openai.OpenAiChatOptions
import org.springframework.ai.openai.api.OpenAiApi

class Agent {

}

fun main() {
    val model: ChatModel = OpenAiChatModel.builder()
        .openAiApi(
            OpenAiApi.builder()
                .baseUrl("https://yunyi.rdzhvip.com/codex")
                .apiKey("4YF935JE-N95N-ZN7T-HF80-ZNBVYZ3W1U8Y")
                .build()
        )
        .defaultOptions(
            OpenAiChatOptions.builder()
                .model("gpt-5.3-codex")
                .temperature(0.2)
                .build()
        )
        .build()

    val disposable = model.stream(
        Prompt(
            SystemMessage(
                "  Tool name: get_current_datetime\n" +
                        "  Description: 获取当前日期和时间。\n" +
                        "  Input:\n" +
                        "  Output:\n" +
                        "  - iso_datetime (string): ISO-8601 日期时间\n" +
                        "  - date (string): YYYY-MM-DD\n" +
                        "  - time (string): HH:mm:ss\n" +
                        "  - weekday (string): 星期几\n" +
                        "  - timezone (string): 实际使用的时区"
            ), UserMessage("你好，今天是什么时候？")
        )
    )
        .doOnSubscribe { print("stream started $it") }
        .doOnNext { print("chunk: $it\n") }
        .doOnError { print("stream error $it") }
        .doOnComplete { print("stream complete") }
        .doFinally { print("stream finally $it") }
        .blockLast()

    print(disposable)
}