package com.dev.lib.mongo;

import com.dev.lib.entity.CoreEntity;
import com.querydsl.core.annotations.QueryEntity;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.domain.Persistable;
import org.springframework.data.mongodb.core.index.Indexed;

import java.time.LocalDateTime;

/**
 * MongoDB 实体基类
 * <p>
 * 使用时继承此类并添加 @Document 注解：
 *
 * @Document(collection = "users")
 * public class User extends MongoEntity { }
 */
@Getter
@Setter
@QueryEntity
public abstract class MongoEntity extends CoreEntity implements Persistable<Long> {

    @Id
    private Long id;

    @Indexed(unique = true)
    private String bizId;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private Long creatorId;

    private Long modifierId;

    @Version
    @ToString.Include
    @EqualsAndHashCode.Include
    private Integer reversion;

    @Override
    public boolean isNew() {

        return id == null || createdAt == null;
    }

}
