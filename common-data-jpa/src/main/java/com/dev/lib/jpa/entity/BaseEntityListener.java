package com.dev.lib.jpa.entity;

import com.dev.lib.entity.id.IDWorker;
import com.dev.lib.entity.id.IntEncoder;
import com.dev.lib.security.util.SecurityContextHolder;
import com.dev.lib.security.util.UserDetails;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;

import java.time.LocalDateTime;

public class BaseEntityListener {

    @PrePersist
    public void prePersist(JpaEntity entity) {
        LocalDateTime now = LocalDateTime.now();

        // 1. 生成 ID
        if (entity.getId() == null) {
            entity.setId(IDWorker.nextID());
        }
        if (entity.getBizId() == null) {
            entity.setBizId(IntEncoder.encode36(entity.getId()));
        }

        if (entity.getDeleted() == null) {
            entity.setDeleted(false);
        }

        // 2. 设置创建时间
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);

        // 3. 设置创建人信息
        UserDetails user = SecurityContextHolder.current();

        if (entity.getCreatorId() == null) {
            entity.setCreatorId(user.getId());
        }
        if (entity.getModifierId() == null) {
            entity.setModifierId(user.getId());
        }
    }

    @PreUpdate
    public void preUpdate(JpaEntity entity) {
        // 1. 更新时间
        entity.setUpdatedAt(LocalDateTime.now());

        // 2. 设置更新人信息
        UserDetails user = SecurityContextHolder.current();
        entity.setModifierId(user.getId());
    }

//    @PreRemove
//    public void preRemove(BaseEntity entity) {
//        // 软删除
//        entity.setDeleted(true);
//        entity.setUpdatedAt(LocalDateTime.now());
//
//        // 记录删除人
//        UserDetails user = SecurityContextHolder.current();
//        if (user.isRealUser()) {
//            entity.setUpdatedBy(user.getUsername());
//            entity.setUpdatedById(user.getId());
//        }
//    }
}