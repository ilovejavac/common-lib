package com.dev.lib.rocketmq

import com.dev.lib.mq.MQ
import com.dev.lib.task.api.TaskClient
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
        taskClient: TaskClient
    ): MQTemplateInitializer {
        return MQTemplateInitializer(template, taskClient)
    }
}

class MQTemplateInitializer(
    template: RocketMQTemplate,
    taskClient: TaskClient
) {
    init {
        MQ.init(RocketMQTemplate(template, taskClient))
    }
}
