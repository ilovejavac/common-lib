package com.dev.lib.jpa.entity;

import org.springframework.aop.framework.Advised;

// 工具类
public final class RepositoryUtils {
    
    private RepositoryUtils() {}
    
    @SuppressWarnings("unchecked")
    public static <T extends JpaEntity> BaseRepositoryImpl<T> unwrap(BaseRepository<T> repository) {
        try {
            if (repository instanceof Advised advised) {
                return (BaseRepositoryImpl<T>) advised.getTargetSource().getTarget();
            }
            return (BaseRepositoryImpl<T>) repository;
        } catch (Exception e) {
            throw new IllegalStateException("无法获取 Repository 实现", e);
        }
    }
}
