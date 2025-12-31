package com.dev.lib.domain;

import com.dev.lib.web.BaseVO;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.eclipse.collections.impl.factory.Lists;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

@EqualsAndHashCode
public abstract class AggregateRoot extends BaseVO {

    @Getter
    @Setter
    private Long id;

    private final List<DomainEvent> domainEvents = Lists.mutable.empty();

    Collection<DomainEvent> domainEvents() {

        return Collections.unmodifiableList(domainEvents);
    }

    protected <T extends DomainEvent> void registerEvent(T... events) {

        this.domainEvents.addAll(Arrays.asList(events));
    }

    void clearDomainEvents() {

        this.domainEvents.clear();
    }

    public void publishAndClear() {

        DomainEventPublisher.publishAndClear(this);
    }

}
