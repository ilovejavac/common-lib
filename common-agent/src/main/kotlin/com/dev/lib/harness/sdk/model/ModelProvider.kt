package com.dev.lib.harness.sdk.model

import com.github.benmanes.caffeine.cache.Caffeine
import org.springframework.stereotype.Component

data class ModelProvider(
    val storage: ModelStorage
) {
    private val modelCache = Caffeine.newBuilder().build<String, LlmClient>()

    fun model(id: String): LlmClient = modelCache.get(id) { key ->
        storage.getModel(key)
    }

}

interface ModelStorage {
    fun getModel(id: String): LlmClient
}