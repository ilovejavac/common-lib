package com.dev.lib.domain;

import org.eclipse.collections.impl.factory.Lists;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public abstract class AggregateRoot {

    private final List<DomainEvent> domainEvents = Lists.mutable.empty();

    protected Collection<DomainEvent> domainEvents() {

        return Collections.unmodifiableList(domainEvents);
    }

    protected <T extends DomainEvent> void registerEvent(T... events) {

        this.domainEvents.addAll(Arrays.asList(events));
    }

    protected void clearDomainEvents() {

        this.domainEvents.clear();
    }

}
