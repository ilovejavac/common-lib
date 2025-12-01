package com.dev.lib.rabbit;

import com.dev.lib.local.task.message.domain.adapter.IRabbitPublish;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class RabbitMQ implements IRabbitPublish {
    private RabbitTemplate template;

    @Override
    public void publish(String exchange, String routingKey, String message) {
        try {

        } finally {

        }
    }
}
