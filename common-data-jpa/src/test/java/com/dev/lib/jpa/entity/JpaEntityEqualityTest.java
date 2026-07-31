package com.dev.lib.jpa.entity;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JpaEntityEqualityTest {

    @Test
    void transientEntitiesShouldOnlyEqualThemselves() {

        EqualityEntity first = new EqualityEntity();
        EqualityEntity second = new EqualityEntity();

        assertThat(first).isNotEqualTo(second);
        assertThat(first).isEqualTo(first);
    }

    @Test
    void persistedIdentityShouldDriveEqualityAndHash() {

        EqualityEntity entity = new EqualityEntity();
        int transientHash = entity.hashCode();

        entity.setId(42L);
        entity.setBizId("16");
        entity.setDeleted(0L);

        EqualityEntity sameIdentity = new EqualityEntity();
        sameIdentity.setId(42L);

        EqualityEntity otherIdentity = new EqualityEntity();
        otherIdentity.setId(43L);

        assertThat(entity).isEqualTo(sameIdentity);
        assertThat(entity.hashCode()).isEqualTo(sameIdentity.hashCode());
        assertThat(entity.hashCode()).isNotEqualTo(transientHash);
        assertThat(entity.hashCode()).isNotEqualTo(otherIdentity.hashCode());
    }

    private static final class EqualityEntity extends JpaEntity {
    }
}
