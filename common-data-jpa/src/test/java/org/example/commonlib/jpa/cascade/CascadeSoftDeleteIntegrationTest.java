package org.example.commonlib.jpa.cascade;

import com.dev.lib.entity.dsl.DslQuery;
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

class CascadeSoftDeleteIntegrationTest {

    private final WebApplicationContextRunner contextRunner = new WebApplicationContextRunner()
            .withUserConfiguration(CascadeSoftDeleteApplication.class)
            .withPropertyValues(
                    "spring.datasource.url=jdbc:h2:mem:cascade_soft_delete;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
                    "spring.datasource.driver-class-name=org.h2.Driver",
                    "spring.datasource.username=sa",
                    "spring.datasource.password=",
                    "spring.jpa.hibernate.ddl-auto=create-drop",
                    "spring.jpa.open-in-view=false",
                    "spring.jpa.show-sql=true",
                    "spring.application.name=cascade-soft-delete-test"
            );

    @Test
    void deleteAllEntitiesShouldSoftDeleteOneToManyCascadeChildren() {

        contextRunner.run(context -> {
            assertThat(context).hasNotFailed();

            CascadeParentRepo parentRepo = context.getBean(CascadeParentRepo.class);
            CascadeChildRepo childRepo = context.getBean(CascadeChildRepo.class);

            CascadeParent parent = new CascadeParent();
            CascadeChild child = new CascadeChild();
            parent.addChild(child);

            parent = parentRepo.saveAndFlush(parent);

            parentRepo.deleteAll(List.of(parent));

            assertThat(parentRepo.onlyDeleted().count()).isEqualTo(1);
            assertThat(childRepo.onlyDeleted().count()).isEqualTo(1);
        });
    }

    @Test
    void deleteEntityShouldSoftDeleteOneToOneCascadeChild() {

        contextRunner.run(context -> {
            assertThat(context).hasNotFailed();

            SingleParentRepo parentRepo = context.getBean(SingleParentRepo.class);
            SingleChildRepo childRepo = context.getBean(SingleChildRepo.class);

            SingleParent parent = new SingleParent();
            parent.setChild(new SingleChild());

            parent = parentRepo.saveAndFlush(parent);

            parentRepo.delete(parent);

            assertThat(parentRepo.onlyDeleted().count()).isEqualTo(1);
            assertThat(childRepo.onlyDeleted().count()).isEqualTo(1);
        });
    }

    @Test
    void deleteDslQueryShouldSoftDeleteCascadeChildren() {

        contextRunner.run(context -> {
            assertThat(context).hasNotFailed();

            CascadeParentRepo parentRepo = context.getBean(CascadeParentRepo.class);
            CascadeChildRepo childRepo = context.getBean(CascadeChildRepo.class);

            CascadeParent parent = new CascadeParent();
            parent.addChild(new CascadeChild());
            parent = parentRepo.saveAndFlush(parent);

            CascadeParentQuery query = new CascadeParentQuery();
            query.setId(parent.getId());
            parentRepo.delete(query);

            assertThat(parentRepo.onlyDeleted().count()).isEqualTo(1);
            assertThat(childRepo.onlyDeleted().count()).isEqualTo(1);
        });
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    static class CascadeSoftDeleteApplication {
    }
}

@Entity
class CascadeParent extends JpaEntity {

    @OneToMany(mappedBy = "parent", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<CascadeChild> children = new ArrayList<>();

    void addChild(CascadeChild child) {

        children.add(child);
        child.setParent(this);
    }
}

@Entity
class CascadeChild extends JpaEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    private CascadeParent parent;

    void setParent(CascadeParent parent) {

        this.parent = parent;
    }
}

interface CascadeParentRepo extends BaseRepository<CascadeParent> {
}

interface CascadeChildRepo extends BaseRepository<CascadeChild> {
}

class CascadeParentQuery extends DslQuery<CascadeParent> {
}

@Entity
class SingleParent extends JpaEntity {

    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private SingleChild child;

    void setChild(SingleChild child) {

        this.child = child;
    }
}

@Entity
class SingleChild extends JpaEntity {
}

interface SingleParentRepo extends BaseRepository<SingleParent> {
}

interface SingleChildRepo extends BaseRepository<SingleChild> {
}
