package com.dev.lib.domain;

import lombok.Setter;
import org.jspecify.annotations.NonNull;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.stereotype.Component;

@Component
public class DomainEventPublisher implements ApplicationEventPublisherAware {

    @Setter
    private static ApplicationEventPublisher publisher;

    /**
     * 发布并清空聚合根的领域事件
     */
    public static void publishAndClear(AggregateRoot root) {

        if (root == null || root.domainEvents().isEmpty()) {
            return;
        }
        root.domainEvents().forEach(publisher::publishEvent);
        root.clearDomainEvents();
    }

    @Override
    public void setApplicationEventPublisher(@NonNull ApplicationEventPublisher applicationEventPublisher) {

        setPublisher(applicationEventPublisher);
    }

}
