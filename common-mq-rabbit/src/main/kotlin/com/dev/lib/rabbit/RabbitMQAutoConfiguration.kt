package com.dev.lib.rabbit

import com.dev.lib.mq.MQ
import org.springframework.amqp.core.AcknowledgeMode
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory
import org.springframework.amqp.rabbit.connection.ConnectionFactory
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.amqp.support.converter.MessageConverter
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Bean

@AutoConfiguration
class RabbitMQAutoConfiguration {

    @Bean
    fun messageConverter(): MessageConverter = FastJsonMessageConverter()

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
        messageConverter: MessageConverter
    ): MQTemplateInitializer {
        return MQTemplateInitializer(template, messageConverter)
    }
}

class MQTemplateInitializer(
    template: RabbitTemplate,
    messageConverter: MessageConverter
) {
    init {
        template.messageConverter = messageConverter
        MQ.init(RabbitMQTemplate(template))
    }
}
