package com.dev.lib.harness.api

import com.alibaba.cloud.ai.graph.NodeOutput
import com.dev.lib.harness.protocol.ResponseEvent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.reactive.asFlow
import reactor.core.publisher.Flux


fun Flux<NodeOutput>.toResponseEventFlow(
    responseId: String
): Flow<ResponseEvent> = flow {
    /**
     * StreamingOutput<T>
     *     -> 根据 outputType / message / state / tokenUsage 归一化
     *     -> 交给一个 ResponseEventAssembler
     *     -> assembler 产出 0~N 个 ResponseEvent
     *     -> emit 给下游
     */


    this@toResponseEventFlow.asFlow().collect { nodeOutput ->

    }

}