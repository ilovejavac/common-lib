package com.dev.lib.entity;

import com.dev.lib.entity.id.IDWorker;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreRemove;

import java.time.LocalDateTime;

public class BaseEntityListener {

    @PrePersist
    public void prePersist(BaseEntity entity) {
        if (entity.getId() == null) {
            entity.setId(IDWorker.nextID());
        }
        if (entity.getBizId() == null) {
            entity.setBizId(IDWorker.newId());
        }
    }

    @PreRemove
    public void softDelete(BaseEntity entity) {
        entity.setDeleted(true);
        entity.setUpdatedAt(LocalDateTime.now());
    }
}