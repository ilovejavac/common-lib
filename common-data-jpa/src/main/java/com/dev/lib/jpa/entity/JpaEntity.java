package com.dev.lib.jpa.entity;

import com.dev.lib.entity.CoreEntity;
import com.dev.lib.jpa.entity.audit.AuditListener;
import com.dev.lib.jpa.entity.encrypt.EncryptionListener;
import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import lombok.Data;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.domain.Persistable;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@MappedSuperclass
@DynamicUpdate
@EntityListeners({BaseEntityListener.class, EncryptionListener.class, AuditListener.class})
public class JpaEntity extends CoreEntity implements Persistable<Long> {

    @Id
    private Long id;

    @Column(nullable = false, length = 12, unique = true, updatable = false)
    private String bizId;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @Column(updatable = false)
    private Long creatorId;

    private Long modifierId;

    @Column(nullable = false)
    private Boolean deleted;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "text")
    private Map<String, Object> features;

    @Override
    public boolean isNew() {

        return createdAt == null;
    }

}
