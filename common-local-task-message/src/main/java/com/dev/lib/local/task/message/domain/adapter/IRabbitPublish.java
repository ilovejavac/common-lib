package com.dev.lib.local.task.message.domain.adapter;

public interface IRabbitPublish {
    void publish(String exchange, String routingKey, String message);
}
