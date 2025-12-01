package com.dev.lib.local.task.message.domain.adapter;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DefaultRabbitMQ implements IRabbitPublish {
    @Override
    public void publish(String exchange, String routingKey, String message) {
        log.warn(
                "No message publisher available, message discarded - exchange: {}, routingKey: {}, message: {}",
                exchange, routingKey, message
        );
    }
}
