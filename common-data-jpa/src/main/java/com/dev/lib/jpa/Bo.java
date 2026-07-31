package com.dev.lib.jpa;

import com.dev.lib.domain.AggregateRoot;
import com.dev.lib.jpa.entity.JpaEntity;

import java.time.LocalDateTime;
import java.util.function.Consumer;

public class Bo<T extends JpaEntity> extends AggregateRoot {

    protected final T entity;

    public Bo(T entity) {

        this.entity = entity;
    }

    public T getEntity() {

        return entity;
    }

    @Override
    public Long getId() {

        return entity == null ? null : entity.getId();
    }

    @Override
    public Bo<T> setId(Long id) {

        takeif(entity -> entity.setId(id));
        return this;
    }

    @Override
    public String getBizId() {

        return entity == null ? null : entity.getBizId();
    }

    @Override
    public Bo<T> setBizId(String bizId) {

        takeif(entity -> entity.setBizId(bizId));
        return this;
    }

    @Override
    public LocalDateTime getCreatedAt() {

        return entity == null ? null : entity.getCreatedAt();
    }

    @Override
    public Bo<T> setCreatedAt(LocalDateTime createdAt) {

        takeif(entity -> entity.setCreatedAt(createdAt));
        return this;
    }

    @Override
    public LocalDateTime getUpdatedAt() {

        return entity == null ? null : entity.getUpdatedAt();
    }

    @Override
    public Bo<T> setUpdatedAt(LocalDateTime updatedAt) {

        takeif(entity -> entity.setUpdatedAt(updatedAt));
        return this;
    }

    @Override
    public Long getCreatorId() {

        return entity == null ? null : entity.getCreatorId();
    }

    @Override
    public Bo<T> setCreatorId(Long creatorId) {

        takeif(entity -> entity.setCreatorId(creatorId));
        return this;
    }

    @Override
    public Long getModifierId() {

        return entity == null ? null : entity.getModifierId();
    }

    @Override
    public Bo<T> setModifierId(Long modifierId) {

        takeif(entity -> entity.setModifierId(modifierId));
        return this;
    }

    public void takeif(Consumer<T> block) {

        if (entity != null) {
            block.accept(entity);
        }
    }
}
