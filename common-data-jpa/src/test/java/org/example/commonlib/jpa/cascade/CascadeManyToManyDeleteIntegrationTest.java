package org.example.commonlib.jpa.cascade;

import com.dev.lib.jpa.entity.BaseRepository;
import com.dev.lib.jpa.entity.JpaEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CascadeManyToManyDeleteIntegrationTest {

    private final WebApplicationContextRunner contextRunner = new WebApplicationContextRunner()
            .withUserConfiguration(CascadeManyToManyDeleteApplication.class)
            .withPropertyValues(
                    "spring.datasource.url=jdbc:h2:mem:cascade_many_to_many_delete;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
                    "spring.datasource.driver-class-name=org.h2.Driver",
                    "spring.datasource.username=sa",
                    "spring.datasource.password=",
                    "spring.jpa.hibernate.ddl-auto=create-drop",
                    "spring.jpa.open-in-view=false",
                    "spring.jpa.show-sql=true",
                    "spring.application.name=cascade-many-to-many-delete-test"
            );

    @Test
    void softDeleteShouldCascadeManyToManyTargetsWhenCascadeAllConfigured() {

        contextRunner.run(context -> {
            assertThat(context).hasNotFailed();

            M2mOwnerRepo ownerRepo = context.getBean(M2mOwnerRepo.class);
            M2mTagRepo tagRepo = context.getBean(M2mTagRepo.class);

            M2mOwner owner = new M2mOwner();
            owner.addTag(new M2mTag("t1"));
            owner = ownerRepo.saveAndFlush(owner);

            ownerRepo.delete(owner);

            assertThat(ownerRepo.onlyDeleted().count()).isEqualTo(1);
            assertThat(tagRepo.onlyDeleted().count()).isEqualTo(1);
            assertThat(ownerRepo.withDeleted().count()).isEqualTo(1);
            assertThat(tagRepo.withDeleted().count()).isEqualTo(1);
        });
    }

    @Test
    void physicalDeleteShouldFollowJpaCascadeForManyToMany() {

        contextRunner.run(context -> {
            assertThat(context).hasNotFailed();

            M2mOwnerRepo ownerRepo = context.getBean(M2mOwnerRepo.class);
            M2mTagRepo tagRepo = context.getBean(M2mTagRepo.class);

            M2mOwner owner = new M2mOwner();
            owner.addTag(new M2mTag("t1"));
            owner = ownerRepo.saveAndFlush(owner);

            ownerRepo.physicalDelete().deleteById(owner.getId());

            assertThat(ownerRepo.withDeleted().count()).isEqualTo(0);
            assertThat(tagRepo.withDeleted().count()).isEqualTo(0);
        });
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    static class CascadeManyToManyDeleteApplication {
    }
}

@Entity
class M2mOwner extends JpaEntity {

    @ManyToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JoinTable(
            name = "m2m_owner_tag",
            joinColumns = @JoinColumn(name = "owner_id"),
            inverseJoinColumns = @JoinColumn(name = "tag_id")
    )
    private List<M2mTag> tags = new ArrayList<>();

    void addTag(M2mTag tag) {

        tags.add(tag);
        tag.addOwner(this);
    }
}

@Entity
class M2mTag extends JpaEntity {

    private String name;

    @ManyToMany(mappedBy = "tags", fetch = FetchType.LAZY)
    private List<M2mOwner> owners = new ArrayList<>();

    public M2mTag() {
    }

    M2mTag(String name) {

        this.name = name;
    }

    void addOwner(M2mOwner owner) {

        owners.add(owner);
    }
}

interface M2mOwnerRepo extends BaseRepository<M2mOwner> {
}

interface M2mTagRepo extends BaseRepository<M2mTag> {
}
