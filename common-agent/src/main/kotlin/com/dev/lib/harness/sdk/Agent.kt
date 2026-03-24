package com.dev.lib.harness.sdk

import com.alibaba.cloud.ai.graph.agent.ReactAgent
import com.dev.lib.CoroutineScopeHolder
import com.dev.lib.harness.protocol.Prompt
import com.dev.lib.harness.protocol.QueryOptions
import com.dev.lib.harness.protocol.SaaResponseEventAdapter
import com.dev.lib.harness.sdk.model.ModelProvider
import kotlinx.coroutines.Job

class Agent(
    val modelProvider: ModelProvider
) {

    fun query(prompt: Prompt, options: QueryOptions): Job {

        val reactAgent = ReactAgent.builder()
            .name("test.ai")
            .model(modelProvider.model(options.turnContext.model).getChatModel())
            .methodTools(*prompt.tools.toTypedArray())
            .returnReasoningContents(true)
            .build()

        val client = reactAgent.stream(prompt.toFormatedInput())

        val adapter = SaaResponseEventAdapter("test", client)

        return CoroutineScopeHolder.launch {
            adapter.sampling {
                println(it)
            }
            // end of launch
        }
    }
}
