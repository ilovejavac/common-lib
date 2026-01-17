package com.dev.lib.rabbit

import com.dev.lib.local.task.message.storage.LocalTaskMessageStorage
import com.dev.lib.mq.MQ
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.ApplicationListener

@AutoConfiguration
class RabbitMQAutoConfiguration(
    private val template: RabbitTemplate,
    private val messageStorage: LocalTaskMessageStorage?
) : ApplicationListener<ApplicationReadyEvent> {

    override fun onApplicationEvent(event: ApplicationReadyEvent) {
        MQ.init(RabbitMQTemplate(template, messageStorage))
    }
}
