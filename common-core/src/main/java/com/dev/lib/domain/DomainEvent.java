package com.dev.lib.domain;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.time.LocalDateTime;

@Getter
public abstract class DomainEvent extends ApplicationEvent {
    private final LocalDateTime occurAt;
    private final String name;
    private final Integer version;

    public DomainEvent(String name, Integer version) {
        super(name);
        this.name = name;
        this.version = version;
        this.occurAt = LocalDateTime.now();
    }
}
