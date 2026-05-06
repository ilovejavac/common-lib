package org.example.commonlib.jpa.security;

import com.dev.lib.jpa.entity.BaseRepository;
import com.dev.lib.jpa.entity.JpaEntity;
import com.dev.lib.security.util.SecurityContextHolder;
import com.dev.lib.security.util.UserDetails;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;

class ScopedWriteProtectionIntegrationTest {

    private final WebApplicationContextRunner contextRunner = new WebApplicationContextRunner()
            .withUserConfiguration(ScopedWriteProtectionApplication.class)
            .withPropertyValues(
                    "spring.datasource.url=jdbc:h2:mem:scoped_write_protection;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
                    "spring.datasource.driver-class-name=org.h2.Driver",
                    "spring.datasource.username=sa",
                    "spring.datasource.password=",
                    "spring.jpa.hibernate.ddl-auto=create-drop",
                    "spring.jpa.open-in-view=false",
                    "spring.jpa.show-sql=true",
                    "spring.application.name=scoped-write-protection-test"
            );

    @Test
    void updateShouldRespectQueryPluginScopeEvenWhenUsingIdCondition() {

        contextRunner.run(context -> {
            assertThat(context).hasNotFailed();
            ScopedWriteThingRepo repo = context.getBean(ScopedWriteThingRepo.class);
            JdbcTemplate jdbcTemplate = context.getBean(JdbcTemplate.class);

            ScopedWriteThing victim = saveAs(repo, 2002L, "before");

            long denied = runAs(1001L, () -> repo.update()
                    .set(ScopedWriteThing::getId, victim.getId())
                    .set(ScopedWriteThing::getName, "hacked")
                    .execute());
            assertThat(denied).isZero();

            String unchanged = queryNameById(jdbcTemplate, victim.getId());
            assertThat(unchanged).isEqualTo("before");

            long allowed = runAs(2002L, () -> repo.update()
                    .set(ScopedWriteThing::getId, victim.getId())
                    .set(ScopedWriteThing::getName, "after")
                    .execute());
            assertThat(allowed).isEqualTo(1L);
            assertThat(queryNameById(jdbcTemplate, victim.getId())).isEqualTo("after");
        });
    }

    @Test
    void deleteByIdAndPhysicalDeleteByIdShouldRespectQueryPluginScope() {

        contextRunner.run(context -> {
            assertThat(context).hasNotFailed();
            ScopedWriteThingRepo repo = context.getBean(ScopedWriteThingRepo.class);
            JdbcTemplate jdbcTemplate = context.getBean(JdbcTemplate.class);

            ScopedWriteThing softVictim = saveAs(repo, 3003L, "soft-victim");
            runAsVoid(1001L, () -> repo.deleteById(softVictim.getId()));
            assertThat(queryDeletedFlag(jdbcTemplate, softVictim.getId())).isFalse();

            runAsVoid(3003L, () -> repo.deleteById(softVictim.getId()));
            assertThat(queryDeletedFlag(jdbcTemplate, softVictim.getId())).isTrue();

            ScopedWriteThing hardVictim = saveAs(repo, 4004L, "hard-victim");
            runAsVoid(1001L, () -> repo.physicalDelete().deleteById(hardVictim.getId()));
            assertThat(queryRowCountById(jdbcTemplate, hardVictim.getId())).isEqualTo(1L);

            runAsVoid(4004L, () -> repo.physicalDelete().deleteById(hardVictim.getId()));
            assertThat(queryRowCountById(jdbcTemplate, hardVictim.getId())).isZero();
        });
    }

    private static ScopedWriteThing saveAs(ScopedWriteThingRepo repo, long ownerId, String name) {

        return runAs(ownerId, () -> {
            ScopedWriteThing entity = new ScopedWriteThing(ownerId, name);
            return repo.saveAndFlush(entity);
        });
    }

    private static <R> R runAs(Long userId, java.util.concurrent.Callable<R> callable) {

        UserDetails user = UserDetails.builder().id(userId).username("u" + userId).build();
        final Object[] holder = new Object[1];
        SecurityContextHolder.with(user, () -> {
            try {
                holder[0] = callable.call();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        @SuppressWarnings("unchecked")
        R result = (R) holder[0];
        return result;
    }

    private static void runAsVoid(Long userId, Runnable runnable) {

        UserDetails user = UserDetails.builder().id(userId).username("u" + userId).build();
        SecurityContextHolder.with(user, runnable);
    }

    private static String queryNameById(JdbcTemplate jdbcTemplate, Long id) {

        return jdbcTemplate.queryForObject(
                "select name from scoped_write_thing where id = ?",
                String.class,
                id
        );
    }

    private static boolean queryDeletedFlag(JdbcTemplate jdbcTemplate, Long id) {

        Boolean deleted = jdbcTemplate.queryForObject(
                "select deleted from scoped_write_thing where id = ?",
                Boolean.class,
                id
        );
        return Boolean.TRUE.equals(deleted);
    }

    private static long queryRowCountById(JdbcTemplate jdbcTemplate, Long id) {

        Long count = jdbcTemplate.queryForObject(
                "select count(*) from scoped_write_thing where id = ?",
                Long.class,
                id
        );
        return count == null ? 0L : count;
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    static class ScopedWriteProtectionApplication {
    }
}

@Entity
@Table(name = "scoped_write_thing")
class ScopedWriteThing extends JpaEntity {

    @Column(nullable = false)
    private Long ownerId;

    @Column(nullable = false, length = 64)
    private String name;

    ScopedWriteThing() {
    }

    ScopedWriteThing(Long ownerId, String name) {

        this.ownerId = ownerId;
        this.name = name;
    }

    public Long getOwnerId() {

        return ownerId;
    }

    public String getName() {

        return name;
    }
}

interface ScopedWriteThingRepo extends BaseRepository<ScopedWriteThing> {
}
