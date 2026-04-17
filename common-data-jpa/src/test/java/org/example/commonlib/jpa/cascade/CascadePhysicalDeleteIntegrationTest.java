package org.example.commonlib.jpa.cascade;

import com.dev.lib.jpa.entity.BaseRepository;
import com.dev.lib.jpa.entity.JpaEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CascadePhysicalDeleteIntegrationTest {

    private final WebApplicationContextRunner contextRunner = new WebApplicationContextRunner()
            .withUserConfiguration(CascadePhysicalDeleteApplication.class)
            .withPropertyValues(
                    "spring.datasource.url=jdbc:h2:mem:cascade_physical_delete;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
                    "spring.datasource.driver-class-name=org.h2.Driver",
                    "spring.datasource.username=sa",
                    "spring.datasource.password=",
                    "spring.jpa.hibernate.ddl-auto=create-drop",
                    "spring.jpa.open-in-view=false",
                    "spring.application.name=cascade-physical-delete-test"
            );

    @Test
    void softDeleteShouldMarkParentAndChildrenDeletedWithoutRemovingRows() {

        contextRunner.run(context -> {
            assertThat(context).hasNotFailed();

            PhysicalParentRepo parentRepo = context.getBean(PhysicalParentRepo.class);
            PhysicalChildRepo childRepo = context.getBean(PhysicalChildRepo.class);

            PhysicalParent parent = new PhysicalParent();
            parent.addChild(new PhysicalChild());
            parent = parentRepo.saveAndFlush(parent);

            parentRepo.delete(parent);

            assertThat(parentRepo.count()).isEqualTo(0);
            assertThat(childRepo.count()).isEqualTo(0);
            assertThat(parentRepo.onlyDeleted().count()).isEqualTo(1);
            assertThat(childRepo.onlyDeleted().count()).isEqualTo(1);
            assertThat(parentRepo.withDeleted().count()).isEqualTo(1);
            assertThat(childRepo.withDeleted().count()).isEqualTo(1);
        });
    }

    @Test
    void physicalDeleteByIdShouldCascadeOneToManyChildren() {

        contextRunner.run(context -> {
            assertThat(context).hasNotFailed();

            PhysicalParentRepo parentRepo = context.getBean(PhysicalParentRepo.class);
            PhysicalChildRepo childRepo = context.getBean(PhysicalChildRepo.class);

            PhysicalParent parent = new PhysicalParent();
            parent.addChild(new PhysicalChild());
            parent = parentRepo.saveAndFlush(parent);

            parentRepo.physicalDelete().deleteById(parent.getId());

            assertThat(parentRepo.withDeleted().count()).isEqualTo(0);
            assertThat(childRepo.withDeleted().count()).isEqualTo(0);
        });
    }

    @Test
    void physicalDeleteByIdShouldCascadeOneToOneChild() {

        contextRunner.run(context -> {
            assertThat(context).hasNotFailed();

            PhysicalSingleParentRepo parentRepo = context.getBean(PhysicalSingleParentRepo.class);
            PhysicalSingleChildRepo childRepo = context.getBean(PhysicalSingleChildRepo.class);

            PhysicalSingleParent parent = new PhysicalSingleParent();
            parent.setChild(new PhysicalSingleChild());
            parent = parentRepo.saveAndFlush(parent);

            parentRepo.physicalDelete().deleteById(parent.getId());

            assertThat(parentRepo.withDeleted().count()).isEqualTo(0);
            assertThat(childRepo.withDeleted().count()).isEqualTo(0);
        });
    }

    @Test
    void physicalDeleteAfterSoftDeleteShouldRemoveSoftDeletedGraph() {

        contextRunner.run(context -> {
            assertThat(context).hasNotFailed();

            PhysicalParentRepo parentRepo = context.getBean(PhysicalParentRepo.class);
            PhysicalChildRepo childRepo = context.getBean(PhysicalChildRepo.class);

            PhysicalParent parent = new PhysicalParent();
            parent.addChild(new PhysicalChild());
            parent = parentRepo.saveAndFlush(parent);

            parentRepo.delete(parent);
            assertThat(parentRepo.onlyDeleted().count()).isEqualTo(1);
            assertThat(childRepo.onlyDeleted().count()).isEqualTo(1);

            parentRepo.physicalDelete().deleteById(parent.getId());

            assertThat(parentRepo.withDeleted().count()).isEqualTo(0);
            assertThat(childRepo.withDeleted().count()).isEqualTo(0);
        });
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    static class CascadePhysicalDeleteApplication {
    }
}

@Entity
class PhysicalParent extends JpaEntity {

    @OneToMany(mappedBy = "parent", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<PhysicalChild> children = new ArrayList<>();

    void addChild(PhysicalChild child) {

        children.add(child);
        child.setParent(this);
    }
}

@Entity
class PhysicalChild extends JpaEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    private PhysicalParent parent;

    void setParent(PhysicalParent parent) {

        this.parent = parent;
    }
}

interface PhysicalParentRepo extends BaseRepository<PhysicalParent> {
}

interface PhysicalChildRepo extends BaseRepository<PhysicalChild> {
}

@Entity
class PhysicalSingleParent extends JpaEntity {

    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private PhysicalSingleChild child;

    void setChild(PhysicalSingleChild child) {

        this.child = child;
    }
}

@Entity
class PhysicalSingleChild extends JpaEntity {
}

interface PhysicalSingleParentRepo extends BaseRepository<PhysicalSingleParent> {
}

interface PhysicalSingleChildRepo extends BaseRepository<PhysicalSingleChild> {
}
