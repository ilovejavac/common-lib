package com.dev.lib.jpa.entity;

import com.dev.lib.entity.CoreEntity;
import com.dev.lib.jpa.entity.encrypt.EncryptionListener;
import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.proxy.HibernateProxy;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.domain.Persistable;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Objects;

@Getter
@Setter
@ToString
@MappedSuperclass
@EntityListeners({
        BaseEntityListener.class,
        EncryptionListener.class,
})
public abstract class JpaEntity extends CoreEntity implements Persistable<Long> {

    @Id
    private Long id;

    @Column(nullable = false, length = 15, unique = true, updatable = false)
    private String bizId;

    @Column(nullable = false)
    private Long deleted;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @Column(updatable = false)
    private Long creatorId;

    private Long modifierId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "text")
    private Map<String, Object> attributes;

    @Override
    public boolean isNew() {

        return createdAt == null;
    }

    @Override
    public final boolean equals(Object other) {

        if (this == other) {
            return true;
        }
        if (other == null || effectiveClass(this) != effectiveClass(other)) {
            return false;
        }
        JpaEntity that = (JpaEntity) other;
        Long thisId = getId();
        return thisId != null && Objects.equals(thisId, that.getId());
    }

    @Override
    public final int hashCode() {

        Long id = getId();
        return 31 * effectiveClass(this).hashCode() + (id == null ? 0 : id.hashCode());
    }

    private static Class<?> effectiveClass(Object entity) {

        if (entity instanceof HibernateProxy proxy) {
            return proxy.getHibernateLazyInitializer().getPersistentClass();
        }
        return entity.getClass();
    }

}
