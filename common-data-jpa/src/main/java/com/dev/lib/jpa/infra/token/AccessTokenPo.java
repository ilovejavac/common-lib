package com.dev.lib.jpa.infra.token;

import com.dev.lib.entity.EntityStatus;
import com.dev.lib.jpa.entity.JpaEntity;
import com.dev.lib.security.model.TokenItem;
import com.dev.lib.security.model.TokenType;
import io.github.linpeilie.annotations.AutoMapper;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Data;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.List;
import java.util.Map;

@Data
@Entity
@Table(name = "sys_access_token", indexes = {
        @Index(columnList = "tokenType, tokenKey", unique = true),
        @Index(columnList = "userId"),
        @Index(columnList = "clientId"),
        @Index(columnList = "expireTime") // 添加过期时间索引，便于清理
})
@AutoMapper(target = TokenItem.class)
public class AccessTokenPo extends JpaEntity {

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "text")
    private List<String> permissions;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "text")
    private List<String> roles;

    private String userId;

    private String tokenKey;

    @Column(columnDefinition = "text")
    private String tokenValue;

    @Enumerated(EnumType.STRING)
    private EntityStatus status;

    @Enumerated(EnumType.STRING)
    private TokenType tokenType;

    // 改为过期时间戳，更利于数据库操作
    private Long expireTime;

    private String clientId;

    // 可选：添加元数据字段
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "text")
    private Map<String, Object> metadata;
}