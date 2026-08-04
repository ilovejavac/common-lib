package com.dev.lib.kafka

import com.dev.lib.mq.MQ
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Bean
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory
import org.springframework.kafka.core.ConsumerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.listener.ContainerProperties

@AutoConfiguration
class KafkaMQAutoConfiguration {

    @Bean
    fun kafkaListenerContainerFactory(consumerFactory: ConsumerFactory<String, Any>): ConcurrentKafkaListenerContainerFactory<String, Any> {
        val factory = ConcurrentKafkaListenerContainerFactory<String, Any>()
        factory.setConsumerFactory(consumerFactory)
        factory.containerProperties.ackMode = ContainerProperties.AckMode.MANUAL
        return factory
    }

    @Bean
    @ConditionalOnMissingBean
    fun mqTemplateInitializer(
        template: KafkaTemplate<String, Any>
    ): MQTemplateInitializer {
        return MQTemplateInitializer(template)
    }
}

class MQTemplateInitializer(
    template: KafkaTemplate<String, Any>
) {
    init {
        MQ.init(KafkaMQTemplate(template))
    }
}
