package com.dev.lib.rocketmq

import com.dev.lib.local.task.message.storage.LocalTaskMessageStorage
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
        messageStorage: LocalTaskMessageStorage?
    ): MQTemplateInitializer {
        return MQTemplateInitializer(template, messageStorage)
    }
}

class MQTemplateInitializer(
    template: RocketMQTemplate,
    messageStorage: LocalTaskMessageStorage?
) {
    init {
        MQ.init(RocketMQTemplate(template, messageStorage))
    }
}
