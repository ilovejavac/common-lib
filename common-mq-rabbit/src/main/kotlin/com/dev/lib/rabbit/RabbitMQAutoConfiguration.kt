package com.dev.lib.rabbit

import com.dev.lib.local.task.message.storage.LocalTaskMessageStorage
import com.dev.lib.mq.MQ
import org.springframework.amqp.core.AcknowledgeMode
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory
import org.springframework.amqp.rabbit.connection.ConnectionFactory
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter
import org.springframework.amqp.support.converter.MessageConverter
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Bean

@AutoConfiguration
class RabbitMQAutoConfiguration {

    @Bean
    fun messageConverter(): MessageConverter = Jackson2JsonMessageConverter()

    @Bean
    fun rabbitListenerContainerFactory(
        connectionFactory: ConnectionFactory,
        messageConverter: MessageConverter
    ): SimpleRabbitListenerContainerFactory {
        return SimpleRabbitListenerContainerFactory().apply {
            setConnectionFactory(connectionFactory)
            setMessageConverter(messageConverter)
            setAcknowledgeMode(AcknowledgeMode.MANUAL)
        }
    }

    @Bean
    @ConditionalOnMissingBean
    fun mqTemplateInitializer(
        template: RabbitTemplate,
        messageStorage: LocalTaskMessageStorage?,
        messageConverter: MessageConverter
    ): MQTemplateInitializer {
        return MQTemplateInitializer(template, messageStorage, messageConverter)
    }
}

class MQTemplateInitializer(
    template: RabbitTemplate,
    messageStorage: LocalTaskMessageStorage?,
    messageConverter: MessageConverter
) {
    init {
        template.messageConverter = messageConverter
        MQ.init(RabbitMQTemplate(template, messageStorage))
    }
}

