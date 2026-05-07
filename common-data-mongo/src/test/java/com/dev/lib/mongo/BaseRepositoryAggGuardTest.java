package com.dev.lib.mongo;

import com.dev.lib.entity.dsl.DslQuery;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import com.querydsl.core.types.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.lang.invoke.MethodHandles;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BaseRepositoryAggGuardTest {

    private CapturingMethodHandler handler;

    private BaseRepository<TestMongoEntity> repository;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {

        handler = new CapturingMethodHandler();
        repository = (BaseRepository<TestMongoEntity>) Proxy.newProxyInstance(
                BaseRepository.class.getClassLoader(),
                new Class<?>[]{
                        BaseRepository.class
                },
                handler
        );
    }

    @Test
    void shouldRejectAggQueryInNormalMongoLoadFlow() {

        TestMongoQuery query = new TestMongoQuery();
        query.agg(MongoAggResult.class)
                .count(TestMongoEntity::getBizId)
                .to(MongoAggResult::setCountValue);

        assertThatThrownBy(() -> repository.load(query))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("agg()")
                .hasMessageContaining("load");
    }

    @Test
    void pageWithoutDslQueryShouldUseDefaultPageable() {

        Page<TestMongoEntity> page = repository.page(null);

        assertThat(page.getContent()).isEmpty();
        assertThat(handler.pageable()).isNotNull();
        assertThat(handler.pageable().getOffset()).isZero();
        assertThat(handler.pageable().getPageSize()).isEqualTo(128);
    }

    @Test
    void loadShouldExcludeSoftDeletedByDefault() {

        repository.load(null);

        assertThat(handler.predicateText()).contains("deletedAt");
        assertThat(handler.predicateText()).contains("is null");
    }

    @Test
    void withDeletedLoadShouldNotAppendDeletedFilter() {

        repository.withDeleted().load(null);

        assertThat(handler.predicateText()).doesNotContain("deletedAt");
    }

    @Test
    void onlyDeletedLoadShouldAppendDeletedFilter() {

        repository.onlyDeleted().load(null);

        assertThat(handler.predicateText()).contains("deletedAt");
        assertThat(handler.predicateText()).contains("is not null");
    }

    @Test
    void deleteWithoutBusinessConditionShouldFailBeforeDeletingEverything() {

        assertThatThrownBy(() -> repository.delete((DslQuery<TestMongoEntity>) null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("批量删除必须指定业务条件");
    }

    static class TestMongoEntity extends MongoEntity {
    }

    static class TestMongoQuery extends DslQuery<TestMongoEntity> {
    }

    static class MongoAggResult {

        private Long countValue;

        public void setCountValue(Long countValue) {

            this.countValue = countValue;
        }
    }

    static class CapturingMethodHandler implements InvocationHandler {

        private final AtomicReference<Pageable> pageable = new AtomicReference<>();

        private final AtomicReference<String> predicateText = new AtomicReference<>();

        Pageable pageable() {

            return pageable.get();
        }

        String predicateText() {

            return predicateText.get();
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {

            if (method.isDefault()) {
                Class<?> declaringClass = method.getDeclaringClass();
                MethodHandles.Lookup lookup = MethodHandles.privateLookupIn(declaringClass, MethodHandles.lookup());
                Object[] actualArgs = args == null ? new Object[0] : args;
                return lookup.unreflectSpecial(method, declaringClass)
                        .bindTo(proxy)
                        .invokeWithArguments(actualArgs);
            }

            if ("findAll".equals(method.getName()) && args != null && args.length == 2 && args[1] instanceof Pageable pageRequest) {
                predicateText.set(String.valueOf(args[0]));
                pageable.set(pageRequest);
                return new PageImpl<>(List.of(), pageRequest, 0);
            }
            if ("findBy".equals(method.getName()) && args != null && args.length == 2 && args[0] instanceof Predicate predicate) {
                predicateText.set(String.valueOf(predicate));
                return Optional.empty();
            }
            throw new UnsupportedOperationException("unexpected call: " + method.getName());
        }
    }
}
