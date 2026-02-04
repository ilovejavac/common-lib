package com.dev.lib.rocketmq

import com.dev.lib.local.task.message.poller.core.PollerEngineRegistry
import com.dev.lib.mq.MQ
import org.apache.rocketmq.spring.core.RocketMQTemplate
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Bean

@AutoConfiguration
class RocketMQAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    fun mqTemplateInitializer(
        template: RocketMQTemplate,
        pollerRegistry: PollerEngineRegistry?
    ): MQTemplateInitializer {
        return MQTemplateInitializer(template, pollerRegistry)
    }
}

class MQTemplateInitializer(
    template: RocketMQTemplate,
    pollerRegistry: PollerEngineRegistry?
) {
    init {
        MQ.init(RocketMQTemplate(template, pollerRegistry))
    }
}
