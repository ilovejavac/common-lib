package com.dev.lib.domain;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public abstract class AggregateRoot {
    private final List<DomainEvent> events = Lists.newArrayList();

    public Collection<? extends DomainEvent> domainEvents() {
        return ImmutableList.copyOf(events);
    }

    protected void addEvent(DomainEvent ...events) {
        this.events.addAll(Arrays.asList(events));
    }

    public void clearEvents() {
        events.clear();
    }
}
