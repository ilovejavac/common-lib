package com.dev.lib.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Where;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.io.Serializable;
import java.time.LocalDateTime;

@Getter
@Setter
@Where(clause = "deleted = false")
@MappedSuperclass
@EntityListeners({AuditingEntityListener.class, BaseEntityListener.class, EncryptionListener.class})
public abstract class BaseEntity implements Serializable {

    @Id
    private Long id;

    @Column(nullable = false, length = 12, unique = true)
    private String bizId;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @CreatedBy
    @Column(length = 64, updatable = false)
    private String createdBy;

    @LastModifiedBy
    @Column(length = 64)
    private String updatedBy;

    @Column(nullable = false)
    private Boolean deleted = false;

    @Version
    private Integer version;
}