package com.dev.lib

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.springframework.beans.factory.DisposableBean
import org.springframework.beans.factory.InitializingBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class KotlinAutoConfig : InitializingBean, DisposableBean {
    private val globalScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    @Bean
    fun scope() = globalScope

    override fun afterPropertiesSet() {
        CoroutineScopeHolder.initGlobalScope(globalScope)
    }

    override fun destroy() {
        CoroutineScopeHolder.destroyGlobalScope()
    }
}