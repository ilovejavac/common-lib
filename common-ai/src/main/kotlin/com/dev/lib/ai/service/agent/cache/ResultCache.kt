package com.dev.lib.ai.service.agent.cache

import com.github.benmanes.caffeine.cache.Caffeine
import java.time.Duration

/**
 * 缓存对话
 */
class ResultCache {
    val cache = Caffeine.newBuilder()
        .expireAfterWrite(Duration.ofMinutes(5))
        .build<String, String>()
}