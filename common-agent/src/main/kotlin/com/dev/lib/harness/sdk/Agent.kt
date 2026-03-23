package com.dev.lib.harness.sdk

import com.alibaba.cloud.ai.graph.agent.ReactAgent
import com.dev.lib.CoroutineScopeHolder
import com.dev.lib.harness.api.toResponseEventFlow
import com.dev.lib.harness.protocol.RequestInput
import com.dev.lib.harness.sdk.model.ModelProvider
import kotlinx.coroutines.Job

class Agent(
    val modelProvider: ModelProvider
) {

    fun query(input: RequestInput): Job {

        val reactAgent = ReactAgent.builder()
            .name("test.ai")
            .model(modelProvider.model(input.model).getChatModel())
            .methodTools(*input.tools.toTypedArray())
            .build()

        val stream = reactAgent.stream(input.message)

        return CoroutineScopeHolder.launch {
            stream.toResponseEventFlow("").collect {
                println(it)
            }
        }

    }
}
