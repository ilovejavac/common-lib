package com.dev.lib.rabbit;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RabbitSerializationOwnershipTest {

    @Test
    void shouldLeaveRabbitSerializationToApplicationConfiguration() {

        List<String> beanMethods = Arrays.stream(RabbitMQAutoConfiguration.class.getDeclaredMethods())
                .map(method -> method.getName())
                .toList();
        List<String> parameterTypes = Arrays.stream(RabbitMQAutoConfiguration.class.getDeclaredMethods())
                .flatMap(method -> Arrays.stream(method.getParameterTypes()))
                .map(Class::getName)
                .toList();

        assertThat(beanMethods).doesNotContain("messageConverter");
        assertThat(parameterTypes).doesNotContain("org.springframework.amqp.support.converter.MessageConverter");
        assertThatThrownBy(() -> Class.forName("com.dev.lib.rabbit.TrustedJacksonRabbitTypeMapper"))
                .isInstanceOf(ClassNotFoundException.class);
    }
}
