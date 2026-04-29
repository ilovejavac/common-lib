package com.dev.lib.jpa.entity.dsl;

import com.dev.lib.jpa.entity.JpaEntity;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.PathMetadata;
import com.querydsl.core.types.dsl.EntityPathBase;
import org.junit.jupiter.api.Test;

import static com.querydsl.core.types.PathMetadataFactory.forVariable;
import static org.assertj.core.api.Assertions.assertThat;

class EntityPathManagerTest {

    @Test
    void resolvesTopLevelEntityPath() {

        EntityPathBase<EntityPathManagerTopLevelEntity> path = EntityPathManager.getEntityPath(
                EntityPathManagerTopLevelEntity.class
        );

        assertThat(path).isSameAs(QEntityPathManagerTopLevelEntity.entityPathManagerTopLevelEntity);
        assertThat(path.getType()).isEqualTo(EntityPathManagerTopLevelEntity.class);
    }

    @Test
    void resolvesNestedStaticEntityPath() {

        EntityPathBase<EntityPathManagerNestedEntityContainer.Entity> path = EntityPathManager.getEntityPath(
                EntityPathManagerNestedEntityContainer.Entity.class
        );

        assertThat(path).isSameAs(QEntityPathManagerNestedEntityContainer_Entity.entity);
        assertThat(path.getType()).isEqualTo(EntityPathManagerNestedEntityContainer.Entity.class);
    }

}

class EntityPathManagerTopLevelEntity extends JpaEntity {

}

class QEntityPathManagerTopLevelEntity extends EntityPathBase<EntityPathManagerTopLevelEntity> {

    public static final QEntityPathManagerTopLevelEntity entityPathManagerTopLevelEntity =
            new QEntityPathManagerTopLevelEntity("entityPathManagerTopLevelEntity");

    QEntityPathManagerTopLevelEntity(String variable) {

        super(EntityPathManagerTopLevelEntity.class, forVariable(variable));
    }

    QEntityPathManagerTopLevelEntity(Path<? extends EntityPathManagerTopLevelEntity> path) {

        super(path.getType(), path.getMetadata());
    }

    QEntityPathManagerTopLevelEntity(PathMetadata metadata) {

        super(EntityPathManagerTopLevelEntity.class, metadata);
    }

}

class EntityPathManagerNestedEntityContainer {

    private EntityPathManagerNestedEntityContainer() {

    }

    static class Entity extends JpaEntity {

    }

}

class QEntityPathManagerNestedEntityContainer_Entity
        extends EntityPathBase<EntityPathManagerNestedEntityContainer.Entity> {

    public static final QEntityPathManagerNestedEntityContainer_Entity entity =
            new QEntityPathManagerNestedEntityContainer_Entity("entity");

    QEntityPathManagerNestedEntityContainer_Entity(String variable) {

        super(EntityPathManagerNestedEntityContainer.Entity.class, forVariable(variable));
    }

    QEntityPathManagerNestedEntityContainer_Entity(Path<? extends EntityPathManagerNestedEntityContainer.Entity> path) {

        super(path.getType(), path.getMetadata());
    }

    QEntityPathManagerNestedEntityContainer_Entity(PathMetadata metadata) {

        super(EntityPathManagerNestedEntityContainer.Entity.class, metadata);
    }

}
