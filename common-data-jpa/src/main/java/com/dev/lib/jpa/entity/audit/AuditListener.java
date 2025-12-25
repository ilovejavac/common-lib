package com.dev.lib.jpa.entity.audit;

import com.alibaba.fastjson2.JSON;
import com.dev.lib.entity.audit.AuditAction;
import com.dev.lib.jpa.entity.JpaEntity;
import com.dev.lib.util.Dispatcher;
import jakarta.persistence.PostPersist;
import jakarta.persistence.PostRemove;
import jakarta.persistence.PostUpdate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuditListener {

    private static AuditRepo auditRepo;

    @Autowired
    public void setDependencies(AuditRepo repo) {

        AuditListener.auditRepo = repo;
    }

    @PostPersist
    public void afterInsert(Object entity) {

        if (entity instanceof JpaEntity base && !(entity instanceof AuditLog)) {
//            save(base, AuditAction.INSERT, toJson(entity));
        }
    }

    @PostUpdate
    public void afterUpdate(Object entity) {

        if (entity instanceof JpaEntity base && !(entity instanceof AuditLog)) {
//            save(base, AuditAction.UPDATE, toJson(entity));
        }
    }

    @PostRemove
    public void afterDelete(Object entity) {

        if (entity instanceof JpaEntity base && !(entity instanceof AuditLog)) {
//            save(base, AuditAction.DELETE, toJson(entity));
        }
    }

    private void save(JpaEntity entity, AuditAction action, String values) {

        CompletableFuture.runAsync(
                () -> {
                    try {
                        AuditLog log = new AuditLog();
                        log.setEntityType(entity.getClass().getSimpleName());
                        log.setEntityId(entity.getId());
                        log.setBizId(entity.getBizId());
                        log.setAction(action);
                        log.setRecordValue(values);
//                        log.setCreatedAt(LocalDateTime.now());
                        log.setUpdatedAt(LocalDateTime.now());
                        log.setCreatorId(entity.getCreatorId());
                        log.setModifierId(entity.getModifierId());

                        auditRepo.save(log);
                    } catch (Exception e) {
                        // 审计失败不影响业务
                        log.warn("audit log error", e);
                    }
                }, Dispatcher.IO
        );
    }

    private String toJson(Object obj) {

        try {
            return JSON.toJSONString(obj);
        } catch (Exception e) {
            return "{}";
        }
    }

}