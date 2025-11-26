package com.dev.lib.jpa.entity;

import com.dev.lib.entity.CoreEntity;
import com.dev.lib.jpa.entity.audit.AuditListener;
import com.dev.lib.jpa.entity.encrypt.EncryptionListener;
import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.Version;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.Map;

@Getter
@Setter
@MappedSuperclass
@EntityListeners({BaseEntityListener.class, EncryptionListener.class, AuditListener.class, OnDeleteListener.class})
public abstract class JpaEntity extends CoreEntity {
    @Id
    @Column(length = 20)
    private Long id;

    @Column(nullable = false, length = 12, unique = true)
    private String bizId;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @Column(updatable = false, length = 20)
    private Long creatorId;

    @Column(length = 20)
    private Long modifierId;

    @Column(nullable = false)
    private Boolean deleted;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "text")
    private Map<String, Object> features;

    @Version
    @ToString.Include
    @EqualsAndHashCode.Include
    @Column(length = 11)
    private Integer reversion = 0;
}
