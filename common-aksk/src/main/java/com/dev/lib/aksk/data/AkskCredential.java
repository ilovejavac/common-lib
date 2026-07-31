package com.dev.lib.aksk.data;

import com.dev.lib.aksk.domain.model.AkskStatus;
import com.dev.lib.aksk.domain.model.dto.AkskDTO;
import com.dev.lib.aksk.domain.model.vo.AkskVO;
import com.dev.lib.entity.dsl.Condition;
import com.dev.lib.entity.dsl.DslQuery;
import com.dev.lib.entity.dsl.QueryType;
import com.dev.lib.entity.encrypt.Encrypt;
import com.dev.lib.jpa.entity.BaseRepository;
import com.dev.lib.jpa.entity.JpaEntity;
import com.dev.lib.util.StringUtils;
import io.github.linpeilie.annotations.AutoMapper;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

public class AkskCredential {

    private AkskCredential() {

    }

    @Getter
    @Setter
    @jakarta.persistence.Entity
    @AutoMapper(target = AkskDTO.Create.class, convertGenerate = false)
    @AutoMapper(target = AkskDTO.Update.class, convertGenerate = false)
    @AutoMapper(target = AkskVO.ListItem.class, reverseConvertGenerate = false)
    @AutoMapper(target = AkskVO.Detail.class, reverseConvertGenerate = false)
    @Table(name = "sys_aksk_credential", indexes = {
            @Index(name = "idx_aksk_access_key", columnList = "access_key", unique = true),
            @Index(name = "idx_aksk_status", columnList = "status")
    })
    public static class Entity extends JpaEntity {

        @Column(nullable = false, unique = true, length = 96)
        private String accessKey;

        @Encrypt
        @Column(nullable = false, length = 256)
        private String secretKey;

        @Column(nullable = false, length = 128)
        private String subjectName;

        private String subjectCode;

        private String contactName;

        private String contactPhone;

        private String description;

        @JdbcTypeCode(SqlTypes.JSON)
        @Column(columnDefinition = "text")
        private Set<String> scopes = new LinkedHashSet<>();

        @JdbcTypeCode(SqlTypes.JSON)
        @Column(columnDefinition = "text")
        private Map<String, String> properties = new LinkedHashMap<>();

        @Enumerated(EnumType.STRING)
        @Column(nullable = false, length = 24, columnDefinition = "varchar(24)")
        private AkskStatus status = AkskStatus.active;

        private LocalDateTime expireAt;

        private LocalDateTime lastUsedAt;

        private String lastUsedIp;

    }

    @SuppressWarnings("unused")
    private static final QAkskCredential_Entity q = QAkskCredential_Entity.entity;

    @Repository
    public interface Mapper extends BaseRepository<Entity> {

        default Optional<Entity> findByAccessKey(String accessKey) {

            return load(new Query().setAccessKey(accessKey));
        }

        default Optional<Entity> findByBizId(String bizId) {

            return load(new Query().setBizId(bizId));
        }

        default Optional<Entity> loadByAccessKey(String accessKey) {

            return load(new Query().setAccessKey(accessKey), q.status.eq(AkskStatus.active));
        }

        @Transactional(rollbackFor = Exception.class)
        default boolean markActive(String id) {

            if (StringUtils.isBlank(id)) {
                return false;
            }

            return update()
                    .set(Entity::getStatus, AkskStatus.active)
                    .where(new Query().setBizId(id), q.status.eq(AkskStatus.disable))
                    .execute() > 0;
        }

        @Transactional(rollbackFor = Exception.class)
        default boolean markDisable(String id) {

            if (StringUtils.isBlank(id)) {
                return false;
            }

            return update()
                    .set(Entity::getStatus, AkskStatus.disable)
                    .where(new Query().setBizId(id), q.status.eq(AkskStatus.active))
                    .execute() > 0;
        }

        @Transactional(rollbackFor = Exception.class)
        default boolean touchLastUsed(String id, String ip, LocalDateTime time) {

            if (StringUtils.isBlank(id)) {
                return false;
            }

            return update()
                    .set(Entity::getLastUsedAt, time)
                    .set(Entity::getLastUsedIp, ip)
                    .where(new Query().setBizId(id))
                    .execute() > 0;
        }

    }

    @Getter
    @Setter
    public static class Query extends DslQuery<Entity> {

        private String accessKey;

        @Condition(type = QueryType.LIKE, field = "accessKey")
        private String accessKeyLike;

        @Condition(type = QueryType.LIKE, field = "subjectName")
        private String subjectNameLike;

        private AkskStatus status;

    }

}
