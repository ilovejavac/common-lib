package com.dev.lib.kafka;

import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class KafkaSerializationOwnershipTest {

    @Test
    void shouldLeaveKafkaSerializationToApplicationConfiguration() throws Exception {

        List<String> beanMethods = Arrays.stream(KafkaMQAutoConfiguration.class.getDeclaredMethods())
                .map(method -> method.getName())
                .toList();
        String yaml;
        try (InputStream input = getClass().getClassLoader().getResourceAsStream("application-mq.yaml")) {
            assertThat(input).isNotNull();
            yaml = new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }

        assertThat(beanMethods).doesNotContain("producerFactory", "consumerFactory", "kafkaTemplate");
        assertThat(yaml).doesNotContain("serializer:", "deserializer:", "JacksonJson");
        assertThatThrownBy(() -> Class.forName("com.dev.lib.kafka.TrustedJacksonKafkaTypeMapper"))
                .isInstanceOf(ClassNotFoundException.class);
    }
}
