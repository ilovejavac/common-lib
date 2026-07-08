package com.dev.lib.jpa;

import com.dev.lib.jpa.entity.JpaEntity;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class BoTest {

    @Test
    void shouldDelegateAggregateRootAndBaseVoAccessorsToEntity() {

        TestEntity entity = new TestEntity();
        Bo<TestEntity> bo = new Bo<>(entity);
        LocalDateTime createdAt = LocalDateTime.of(2026, 1, 2, 3, 4, 5);
        LocalDateTime updatedAt = LocalDateTime.of(2026, 2, 3, 4, 5, 6);

        bo.setId(10L);
        bo.setBizId("BIZ-10");
        bo.setCreatedAt(createdAt);
        bo.setUpdatedAt(updatedAt);
        bo.setCreatorId(20L);
        bo.setModifierId(30L);

        assertThat(entity.getId()).isEqualTo(10L);
        assertThat(entity.getBizId()).isEqualTo("BIZ-10");
        assertThat(entity.getCreatedAt()).isEqualTo(createdAt);
        assertThat(entity.getUpdatedAt()).isEqualTo(updatedAt);
        assertThat(entity.getCreatorId()).isEqualTo(20L);
        assertThat(entity.getModifierId()).isEqualTo(30L);
        assertThat(bo.getId()).isEqualTo(10L);
        assertThat(bo.getBizId()).isEqualTo("BIZ-10");
        assertThat(bo.getCreatedAt()).isEqualTo(createdAt);
        assertThat(bo.getUpdatedAt()).isEqualTo(updatedAt);
        assertThat(bo.getCreatorId()).isEqualTo(20L);
        assertThat(bo.getModifierId()).isEqualTo(30L);
    }

    @Test
    void shouldInvokeTakeifOnlyWhenEntityExists() {

        TestEntity entity = new TestEntity();
        Bo<TestEntity> bo = new Bo<>(entity);
        AtomicReference<TestEntity> invokedWith = new AtomicReference<>();

        bo.takeif(invokedWith::set);
        new Bo<TestEntity>(null).takeif(candidate -> invokedWith.set(null));

        assertThat(invokedWith).hasValue(entity);
    }

    static class TestEntity extends JpaEntity {
    }
}
