package com.dev.lib.jpa.entity;

import org.springframework.aop.framework.Advised;

public final class RepositoryUtils {

    private RepositoryUtils() {
    }

    @SuppressWarnings("unchecked")
    public static <T extends JpaEntity> BaseRepositoryImpl<T> unwrap(BaseRepository<T> repository) {

        Object candidate = repository;
        try {
            while (candidate instanceof Advised advised) {
                Object target = advised.getTargetSource().getTarget();
                if (target == null || target == candidate) {
                    break;
                }
                candidate = target;
            }
            if (candidate instanceof BaseRepositoryImpl<?> impl) {
                return (BaseRepositoryImpl<T>) impl;
            }
        } catch (Exception exception) {
            throw new IllegalStateException("无法获取 Repository 实现", exception);
        }
        throw new IllegalStateException("无法获取 Repository 实现: " + repository.getClass().getName());
    }
}
