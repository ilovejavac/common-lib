package com.dev.lib.aksk.data;

import com.dev.lib.aksk.domain.model.AkskStatus;
import com.dev.lib.config.properties.AppSecurityProperties;
import com.dev.lib.jpa.TransactionHelper;
import com.dev.lib.util.encrypt.EncryptionServiceImpl;
import com.dev.lib.util.encrypt.factory.EncryptionStrategyFactory;
import com.dev.lib.util.encrypt.impl.Base64EncryptionStrategy;
import jakarta.persistence.EntityManager;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class AkskCredentialRepositoryIntegrationTest {

    private final WebApplicationContextRunner contextRunner = new WebApplicationContextRunner()
            .withUserConfiguration(AkskCredentialJpaApplication.class)
            .withPropertyValues(
                    "spring.datasource.url=jdbc:h2:mem:aksk_credential_repository;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
                    "spring.datasource.driver-class-name=org.h2.Driver",
                    "spring.datasource.username=sa",
                    "spring.datasource.password=",
                    "spring.jpa.hibernate.ddl-auto=create-drop",
                    "spring.jpa.open-in-view=false",
                    "spring.jpa.properties.hibernate.generate_statistics=true",
                    "spring.application.name=aksk-credential-repository-test",
                    "app.security.encrypt-version=base64"
            );

    @Test
    void shouldPersistJsonFieldsAndSupportNestedRepositoryLifecycle() {

        contextRunner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).hasSingleBean(AkskCredential.Mapper.class);

            AkskCredential.Mapper mapper = context.getBean(AkskCredential.Mapper.class);
            EntityManager entityManager = context.getBean(EntityManager.class);
            JdbcTemplate jdbcTemplate = context.getBean(JdbcTemplate.class);

            AkskCredential.Entity saved = mapper.saveAndFlush(credential("ak_integration"));
            String bizId = saved.getBizId();
            String rawSecretKey = jdbcTemplate.queryForObject(
                    "select secret_key from sys_aksk_credential where id = ?",
                    String.class,
                    saved.getId()
            );
            entityManager.clear();

            AkskCredential.Entity loaded = mapper.findByAccessKey("ak_integration").orElseThrow();
            assertThat(loaded.getBizId()).isEqualTo(bizId);
            assertThat(rawSecretKey).startsWith("v1:");
            assertThat(rawSecretKey).isNotEqualTo("sk_integration");
            assertThat(loaded.getSecretKey()).isEqualTo("sk_integration");
            assertThat(loaded.getScopes()).containsExactlyInAnyOrder("scope:a", "scope:b");
            assertThat(loaded.getProperties())
                    .containsEntry("env", "test")
                    .containsEntry("owner", "aksk");
            assertThat(loaded.getStatus()).isEqualTo(AkskStatus.disable);

            assertThat(mapper.markActive(bizId)).isTrue();
            entityManager.clear();
            assertThat(mapper.findByBizId(bizId)).map(AkskCredential.Entity::getStatus)
                    .contains(AkskStatus.active);

            assertThat(mapper.markDisable(bizId)).isTrue();
            entityManager.clear();
            assertThat(mapper.findByBizId(bizId)).map(AkskCredential.Entity::getStatus)
                    .contains(AkskStatus.disable);

            AkskCredential.Query query = new AkskCredential.Query();
            query.setAccessKey("ak_integration");
            assertThat(mapper.delete(query)).isEqualTo(1L);
            assertThat(mapper.count(query)).isZero();
            assertThat(mapper.onlyDeleted().count(query)).isEqualTo(1L);
        });
    }

    @Test
    void lifecycleHelpersShouldUseBulkUpdatesWithoutLoadingCredentialEntities() {

        contextRunner.run(context -> {
            assertThat(context).hasNotFailed();

            AkskCredential.Mapper mapper = context.getBean(AkskCredential.Mapper.class);
            EntityManager entityManager = context.getBean(EntityManager.class);
            Statistics statistics = entityManager.getEntityManagerFactory()
                    .unwrap(SessionFactory.class)
                    .getStatistics();

            AkskCredential.Entity saved = mapper.saveAndFlush(credential("ak_bulk_update"));
            String bizId = saved.getBizId();
            entityManager.clear();

            statistics.clear();
            assertThat(mapper.markActive(bizId)).isTrue();
            assertNoCredentialEntityLoaded(statistics, "markActive");
            entityManager.clear();
            assertThat(mapper.findByBizId(bizId)).map(AkskCredential.Entity::getStatus)
                    .contains(AkskStatus.active);

            statistics.clear();
            assertThat(mapper.markActive(bizId)).isFalse();
            assertNoCredentialEntityLoaded(statistics, "markActive no-op");

            statistics.clear();
            assertThat(mapper.markDisable(bizId)).isTrue();
            assertNoCredentialEntityLoaded(statistics, "markDisable");
            entityManager.clear();
            assertThat(mapper.findByBizId(bizId)).map(AkskCredential.Entity::getStatus)
                    .contains(AkskStatus.disable);

            LocalDateTime usedAt = LocalDateTime.of(2026, 5, 8, 12, 10);
            statistics.clear();
            assertThat(mapper.touchLastUsed(bizId, "192.0.2.10", usedAt)).isTrue();
            assertNoCredentialEntityLoaded(statistics, "touchLastUsed");
            entityManager.clear();
            AkskCredential.Entity touched = mapper.findByBizId(bizId).orElseThrow();
            assertThat(touched.getLastUsedAt()).isEqualTo(usedAt);
            assertThat(touched.getLastUsedIp()).isEqualTo("192.0.2.10");

            LocalDateTime laterUsedAt = LocalDateTime.of(2026, 5, 8, 12, 15);
            statistics.clear();
            assertThat(mapper.touchLastUsed(bizId, null, laterUsedAt)).isTrue();
            assertNoCredentialEntityLoaded(statistics, "touchLastUsed null ip");
            entityManager.clear();
            AkskCredential.Entity touchedWithoutIp = mapper.findByBizId(bizId).orElseThrow();
            assertThat(touchedWithoutIp.getLastUsedAt()).isEqualTo(laterUsedAt);
            assertThat(touchedWithoutIp.getLastUsedIp()).isEqualTo("192.0.2.10");
        });
    }

    private void assertNoCredentialEntityLoaded(Statistics statistics, String operation) {

        assertThat(statistics.getEntityLoadCount())
                .as(operation + " should update without loading AkskCredential.Entity")
                .isZero();
    }

    private AkskCredential.Entity credential(String accessKey) {

        AkskCredential.Entity entity = new AkskCredential.Entity();
        entity.setAccessKey(accessKey);
        entity.setSecretKey("sk_integration");
        entity.setSubjectName("Integration Subject");
        entity.setSubjectCode("integration");
        entity.setContactName("Ops");
        entity.setContactPhone("10086");
        entity.setDescription("JPA integration test");
        entity.setScopes(new LinkedHashSet<>(Set.of("scope:a", "scope:b")));
        entity.setProperties(new LinkedHashMap<>(Map.of("env", "test", "owner", "aksk")));
        entity.setStatus(AkskStatus.disable);
        entity.setExpireAt(LocalDateTime.now().plusDays(1));
        return entity;
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @EnableConfigurationProperties(AppSecurityProperties.class)
    @Import({
            TransactionHelper.class,
            EncryptionStrategyFactory.class,
            EncryptionServiceImpl.class,
            Base64EncryptionStrategy.class
    })
    static class AkskCredentialJpaApplication {
    }
}
