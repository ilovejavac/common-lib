package com.dev.lib.entity;

import com.dev.lib.entity.id.IDWorker;
import jakarta.persistence.PrePersist;

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
}