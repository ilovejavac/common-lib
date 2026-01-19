package com.dev.lib.kafka

import com.dev.lib.local.task.message.storage.LocalTaskMessageStorage
import com.dev.lib.mq.MQ
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.producer.ProducerConfig
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Bean
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory
import org.springframework.kafka.core.ConsumerFactory
import org.springframework.kafka.core.DefaultKafkaConsumerFactory
import org.springframework.kafka.core.DefaultKafkaProducerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.core.ProducerFactory
import org.springframework.kafka.listener.ContainerProperties
import org.springframework.kafka.support.serializer.JsonDeserializer
import org.springframework.kafka.support.serializer.JsonSerializer

@AutoConfiguration
class KafkaMQAutoConfiguration {

    @Bean
    fun producerFactory(): ProducerFactory<String, Any> {
        val config = mapOf(
            ProducerConfig.BOOTSTRAP_SERVERS_CONFIG to "\${spring.kafka.bootstrap-servers:localhost:9092}",
            ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG to JsonSerializer::class.java,
            ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG to JsonSerializer::class.java,
            ProducerConfig.ACKS_CONFIG to "all"
        )
        return DefaultKafkaProducerFactory(config)
    }

    @Bean
    fun consumerFactory(): ConsumerFactory<String, Any> {
        val config = mapOf(
            ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG to "\${spring.kafka.bootstrap-servers:localhost:9092}",
            ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG to JsonDeserializer::class.java,
            ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG to JsonDeserializer::class.java,
            ConsumerConfig.AUTO_OFFSET_RESET_CONFIG to "earliest",
            ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG to "false"
        )
        return DefaultKafkaConsumerFactory(config)
    }

    @Bean
    fun kafkaTemplate(producerFactory: ProducerFactory<String, Any>): KafkaTemplate<String, Any> {
        return KafkaTemplate(producerFactory)
    }

    @Bean
    fun kafkaListenerContainerFactory(consumerFactory: ConsumerFactory<String, Any>): ConcurrentKafkaListenerContainerFactory<String, Any> {
        val factory = ConcurrentKafkaListenerContainerFactory<String, Any>()
        factory.consumerFactory = consumerFactory
        factory.containerProperties.ackMode = ContainerProperties.AckMode.MANUAL
        return factory
    }

    @Bean
    @ConditionalOnMissingBean
    fun mqTemplateInitializer(
        template: KafkaTemplate<String, Any>,
        messageStorage: LocalTaskMessageStorage?
    ): MQTemplateInitializer {
        return MQTemplateInitializer(template, messageStorage)
    }
}

class MQTemplateInitializer(
    template: KafkaTemplate<String, Any>,
    messageStorage: LocalTaskMessageStorage?
) {
    init {
        MQ.init(KafkaMQTemplate(template, messageStorage))
    }
}
