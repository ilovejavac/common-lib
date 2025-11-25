package com.dev.lib.jpa.entity.sass;

import com.dev.lib.security.util.SecurityContextHolder;
import com.dev.lib.security.util.UserDetails;
import jakarta.persistence.PrePersist;

/**
 * 租户实体监听器
 */
public class TenantEntityListener {

    @PrePersist
    public void prePersist(TenantBaseEntity entity) {
        // 自动填充租户ID
        if (entity.getTenantId() == null) {
            UserDetails user = SecurityContextHolder.current();
            if (user.isRealUser()) {
                entity.setTenantId(user.getTenant());
            } else {
                // 匿名/内部用户使用默认租户
                entity.setTenantId(0L);  // 或抛出异常
            }
        }
    }
}