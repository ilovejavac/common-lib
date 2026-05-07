package com.dev.lib.rabbit

import com.dev.lib.mq.MQ
import com.dev.lib.task.api.TaskClient
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
        taskClient: TaskClient,
        messageConverter: MessageConverter
    ): MQTemplateInitializer {
        return MQTemplateInitializer(template, taskClient, messageConverter)
    }
}

class MQTemplateInitializer(
    template: RabbitTemplate,
    taskClient: TaskClient,
    messageConverter: MessageConverter
) {
    init {
        template.messageConverter = messageConverter
        MQ.init(RabbitMQTemplate(template, taskClient))
    }
}
