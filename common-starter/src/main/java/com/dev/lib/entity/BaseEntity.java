package com.dev.lib.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Where;

import java.io.Serializable;
import java.time.LocalDateTime;

@Getter
@Setter
@Where(clause = "deleted = false")
@MappedSuperclass
@EntityListeners({BaseEntityListener.class, EncryptionListener.class})
public abstract class BaseEntity implements Serializable {

    @Id
    private Long id;

    @Column(nullable = false, length = 12, unique = true)
    private String bizId;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @Column(length = 64, updatable = false)
    private String createdBy;

    @Column(length = 64)
    private String updatedBy;

    @Column(name = "created_by_id", updatable = false)
    private Long createdById;

    @Column(name = "updated_by_id")
    private Long updatedById;

    @Column(nullable = false)
    private Boolean deleted = false;

    @Version
    private Integer version;
}