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

    @Bean
    fun scope() = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun afterPropertiesSet() {
        CoroutineScopeHolder.initGlobalScope(scope())
    }

    override fun destroy() {
        CoroutineScopeHolder.destroyGlobalScope()
    }
}