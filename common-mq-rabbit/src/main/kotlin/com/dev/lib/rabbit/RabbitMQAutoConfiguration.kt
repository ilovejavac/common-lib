package com.dev.lib.rabbit

import com.dev.lib.mq.MQ
import org.springframework.amqp.core.AcknowledgeMode
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory
import org.springframework.amqp.rabbit.connection.ConnectionFactory
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Bean

@AutoConfiguration
class RabbitMQAutoConfiguration {

    @Bean
    fun rabbitListenerContainerFactory(
        connectionFactory: ConnectionFactory
    ): SimpleRabbitListenerContainerFactory {
        return SimpleRabbitListenerContainerFactory().apply {
            setConnectionFactory(connectionFactory)
            setAcknowledgeMode(AcknowledgeMode.MANUAL)
        }
    }

    @Bean
    @ConditionalOnMissingBean
    fun mqTemplateInitializer(
        template: RabbitTemplate
    ): MQTemplateInitializer {
        return MQTemplateInitializer(template)
    }
}

class MQTemplateInitializer(
    template: RabbitTemplate
) {
    init {
        MQ.init(RabbitMQTemplate(template))
    }
}
