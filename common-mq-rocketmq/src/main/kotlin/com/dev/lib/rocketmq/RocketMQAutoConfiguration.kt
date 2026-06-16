package com.dev.lib.rocketmq

import com.dev.lib.mq.MQ
import org.apache.rocketmq.spring.core.RocketMQTemplate as SpringRocketMQTemplate
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Bean

@AutoConfiguration
class RocketMQAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    fun mqTemplateInitializer(
        template: SpringRocketMQTemplate
    ): MQTemplateInitializer {
        return MQTemplateInitializer(template)
    }
}

class MQTemplateInitializer(
    template: SpringRocketMQTemplate
) {
    init {
        MQ.init(RocketMQTemplate(template))
    }
}
