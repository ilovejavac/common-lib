package com.dev.lib

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class KotlinAutoConfig {

    @Bean
    fun scope() = CoroutineScope(Dispatchers.IO + SupervisorJob())
}