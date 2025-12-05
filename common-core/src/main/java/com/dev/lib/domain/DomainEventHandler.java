package com.dev.lib.domain;

public interface DomainEventHandler<T extends DomainEvent> {
    void handle(T entity);
}
