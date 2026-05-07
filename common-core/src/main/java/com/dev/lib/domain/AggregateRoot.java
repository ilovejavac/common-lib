package com.dev.lib.domain;

import com.dev.lib.web.BaseVO;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

@EqualsAndHashCode
public abstract class AggregateRoot extends BaseVO {

    @Getter
    @Setter
    private Long id;

    private final List<DomainEvent> domainEvents = new ArrayList<>();

    Collection<DomainEvent> domainEvents() {

        return domainEvents;
    }

    protected void registerEvent(DomainEvent... events) {

        this.domainEvents.addAll(Arrays.asList(events));
    }

    void clearDomainEvents() {

        this.domainEvents.clear();
    }

    public void emit() {

        DomainEventPublisher.publishAndClear(this);
    }

}
