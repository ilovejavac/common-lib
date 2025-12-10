package com.dev.lib.mongo;

import com.dev.lib.entity.id.IDWorker;
import com.dev.lib.entity.id.IntEncoder;
import com.dev.lib.security.util.SecurityContextHolder;
import com.dev.lib.security.util.UserDetails;
import org.springframework.core.Ordered;
import org.springframework.data.mongodb.core.mapping.event.BeforeConvertCallback;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 等效于 JPA 的 BaseEntityListener
 */
@Component
public class MongoBaseEntityCallback implements BeforeConvertCallback<MongoEntity>, Ordered {

    @Override
    public MongoEntity onBeforeConvert(MongoEntity entity, String collection) {
        LocalDateTime now = LocalDateTime.now();
        UserDetails user = SecurityContextHolder.current();

        if (entity.isNew()) {
            entity.setId(IDWorker.nextID());
            entity.setBizId(IntEncoder.encode36(entity.getId()));
            entity.setCreatedAt(now);
            entity.setCreatorId(user.getId());
        }

        // 更新
        entity.setUpdatedAt(now);
        entity.setModifierId(user.getId());

        return entity;
    }

    @Override
    public int getOrder() {
        return 100;
    }
}
