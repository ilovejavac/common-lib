package org.example.commonlib.jpa.cascade;

import com.dev.lib.entity.dsl.Condition;
import com.dev.lib.entity.dsl.DslQuery;
import com.dev.lib.jpa.entity.BaseRepository;
import com.dev.lib.jpa.entity.JpaEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
                    "spring.jpa.show-sql=false",
                    "spring.application.name=cascade-soft-delete-test"
            );

    @Test
    void deleteEntityShouldSoftDeleteOneToManyChildren() {

        contextRunner.run(context -> {
            assertThat(context).hasNotFailed();

            CascadeParentRepo repository = context.getBean(CascadeParentRepo.class);
            JdbcTemplate jdbcTemplate = context.getBean(JdbcTemplate.class);

            CascadeParent parent = new CascadeParent();
            CascadeChild child = new CascadeChild();
            parent.addChild(child);
            repository.save(parent);

            repository.delete(parent);

            assertThat(repository.load()).isEmpty();
            assertThat(queryDeleted(jdbcTemplate, "cascade_parent", parent.getId())).isEqualTo(parent.getId());
            assertThat(queryDeleted(jdbcTemplate, "cascade_child", child.getId())).isEqualTo(child.getId());
        });
    }

    @Test
    void deleteEntityShouldSoftDeleteOneToOneChild() {

        contextRunner.run(context -> {
            assertThat(context).hasNotFailed();

            SingleParentRepo repository = context.getBean(SingleParentRepo.class);
            JdbcTemplate jdbcTemplate = context.getBean(JdbcTemplate.class);

            SingleParent parent = new SingleParent();
            SingleChild child = new SingleChild();
            parent.setChild(child);
            repository.save(parent);

            repository.delete(parent);

            assertThat(repository.load()).isEmpty();
            assertThat(queryDeleted(jdbcTemplate, "single_parent", parent.getId())).isEqualTo(parent.getId());
            assertThat(queryDeleted(jdbcTemplate, "single_child", child.getId())).isEqualTo(child.getId());
        });
    }

    @Test
    void deleteEntityShouldSoftDeleteManyToManyTargetsWithCascadeRemove() {

        contextRunner.run(context -> {
            assertThat(context).hasNotFailed();

            ManyOwnerRepo ownerRepository = context.getBean(ManyOwnerRepo.class);
            JdbcTemplate jdbcTemplate = context.getBean(JdbcTemplate.class);

            ManyOwner owner = new ManyOwner();
            ManyTag tag = new ManyTag();
            owner.addTag(tag);
            ownerRepository.save(owner);

            ownerRepository.delete(owner);

            assertThat(ownerRepository.load()).isEmpty();
            assertThat(queryDeleted(jdbcTemplate, "cascade_many_owner", owner.getId())).isEqualTo(owner.getId());
            assertThat(queryDeleted(jdbcTemplate, "cascade_many_tag", tag.getId())).isEqualTo(tag.getId());
        });
    }

    @Test
    void deleteByQueryShouldSoftDeleteOnlyMatchingRootsAndTheirChildren() {

        contextRunner.run(context -> {
            assertThat(context).hasNotFailed();

            CascadeParentRepo repository = context.getBean(CascadeParentRepo.class);
            JdbcTemplate jdbcTemplate = context.getBean(JdbcTemplate.class);

            CascadeParent first = new CascadeParent();
            CascadeChild firstChild = new CascadeChild();
            first.addChild(firstChild);
            repository.save(first);

            CascadeParent second = new CascadeParent();
            CascadeChild secondChild = new CascadeChild();
            second.addChild(secondChild);
            repository.save(second);

            long affected = repository.delete(new CascadeParentQuery().setBizId(first.getBizId()));

            assertThat(affected).isEqualTo(1);
            assertThat(repository.load(new CascadeParentQuery().setBizId(first.getBizId()))).isEmpty();
            assertThat(repository.load(new CascadeParentQuery().setBizId(second.getBizId()))).isPresent();
            assertThat(queryDeleted(jdbcTemplate, "cascade_parent", first.getId())).isEqualTo(first.getId());
            assertThat(queryDeleted(jdbcTemplate, "cascade_child", firstChild.getId())).isEqualTo(firstChild.getId());
            assertThat(queryDeleted(jdbcTemplate, "cascade_child", secondChild.getId())).isZero();
        });
    }

    @Test
    void deleteByQueryShouldRejectMissingBusinessCondition() {

        contextRunner.run(context -> {
            assertThat(context).hasNotFailed();

            CascadeParentRepo repository = context.getBean(CascadeParentRepo.class);

            assertThatThrownBy(() -> repository.delete((DslQuery<CascadeParent>) null))
                    .isInstanceOf(InvalidDataAccessApiUsageException.class)
                    .hasMessageContaining("业务条件");
            assertThatThrownBy(() -> repository.delete(new CascadeParentQuery()))
                    .isInstanceOf(InvalidDataAccessApiUsageException.class)
                    .hasMessageContaining("业务条件");
        });
    }

    private static long queryDeleted(JdbcTemplate jdbcTemplate, String tableName, Long id) {

        return jdbcTemplate.queryForObject(
                "select deleted from " + tableName + " where id = ?",
                Long.class,
                id
        );
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

class CascadeParentQuery extends DslQuery<CascadeParent> {

    @Condition(field = "bizId")
    private String bizId;

    public CascadeParentQuery setBizId(String bizId) {

        this.bizId = bizId;
        return this;
    }
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

@Entity
@Table(name = "cascade_many_owner")
class ManyOwner extends JpaEntity {

    @ManyToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JoinTable(
            name = "cascade_many_owner_tag",
            joinColumns = @JoinColumn(name = "owner_id"),
            inverseJoinColumns = @JoinColumn(name = "tag_id")
    )
    private List<ManyTag> tags = new ArrayList<>();

    void addTag(ManyTag tag) {

        tags.add(tag);
        tag.addOwner(this);
    }
}

@Entity
@Table(name = "cascade_many_tag")
class ManyTag extends JpaEntity {

    @ManyToMany(mappedBy = "tags", fetch = FetchType.LAZY)
    private List<ManyOwner> owners = new ArrayList<>();

    void addOwner(ManyOwner owner) {

        owners.add(owner);
    }
}

interface ManyOwnerRepo extends BaseRepository<ManyOwner> {
}
