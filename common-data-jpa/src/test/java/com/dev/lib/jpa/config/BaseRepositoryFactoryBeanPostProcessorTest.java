package com.dev.lib.jpa.config;

import com.dev.lib.jpa.entity.BaseRepository;
import com.dev.lib.jpa.entity.BaseRepositoryImpl;
import com.dev.lib.jpa.entity.JpaEntity;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.support.JpaRepositoryFactoryBean;
import org.springframework.data.repository.core.support.RepositoryFactoryBeanSupport;

import java.lang.reflect.Field;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class BaseRepositoryFactoryBeanPostProcessorTest {

    @Test
    void shouldSetBaseRepositoryImplForBaseRepository() throws Exception {

        BaseRepositoryFactoryBeanPostProcessor processor = new BaseRepositoryFactoryBeanPostProcessor();
        JpaRepositoryFactoryBean<DemoBaseRepository, DemoEntity, Long> factoryBean =
                new JpaRepositoryFactoryBean<>(DemoBaseRepository.class);

        processor.postProcessBeforeInitialization(factoryBean, "demoBaseRepository");

        assertThat(readRepositoryBaseClass(factoryBean)).contains(BaseRepositoryImpl.class);
    }

    @Test
    void shouldSetBaseRepositoryImplForJpaRepositoryOnJpaEntity() throws Exception {

        BaseRepositoryFactoryBeanPostProcessor processor = new BaseRepositoryFactoryBeanPostProcessor();
        JpaRepositoryFactoryBean<DemoPlainRepository, DemoEntity, Long> factoryBean =
                new JpaRepositoryFactoryBean<>(DemoPlainRepository.class);

        processor.postProcessBeforeInitialization(factoryBean, "demoPlainRepository");

        assertThat(readRepositoryBaseClass(factoryBean)).contains(BaseRepositoryImpl.class);
    }

    @Test
    void shouldNotChangeJpaRepositoryForNonJpaEntity() throws Exception {

        BaseRepositoryFactoryBeanPostProcessor processor = new BaseRepositoryFactoryBeanPostProcessor();
        JpaRepositoryFactoryBean<ExternalPlainRepository, ExternalEntity, Long> factoryBean =
                new JpaRepositoryFactoryBean<>(ExternalPlainRepository.class);

        processor.postProcessBeforeInitialization(factoryBean, "externalPlainRepository");

        assertThat(readRepositoryBaseClass(factoryBean))
                .satisfies(baseClass -> assertThat(baseClass.orElse(null)).isNotEqualTo(BaseRepositoryImpl.class));
    }

    @SuppressWarnings("unchecked")
    private Optional<Class<?>> readRepositoryBaseClass(Object factoryBean) throws Exception {

        Field field = RepositoryFactoryBeanSupport.class.getDeclaredField("repositoryBaseClass");
        field.setAccessible(true);
        Object value = field.get(factoryBean);
        if (value == null) {
            return Optional.empty();
        }
        if (value instanceof Optional<?> optional) {
            return (Optional<Class<?>>) optional;
        }
        if (value instanceof Class<?> clazz) {
            return Optional.of(clazz);
        }
        throw new IllegalStateException("Unexpected repositoryBaseClass type: " + value.getClass().getName());
    }

    interface DemoBaseRepository extends BaseRepository<DemoEntity> {
    }

    interface DemoPlainRepository extends JpaRepository<DemoEntity, Long> {
    }

    interface ExternalPlainRepository extends JpaRepository<ExternalEntity, Long> {
    }

    static class DemoEntity extends JpaEntity {
    }

    static class ExternalEntity {
    }
}
