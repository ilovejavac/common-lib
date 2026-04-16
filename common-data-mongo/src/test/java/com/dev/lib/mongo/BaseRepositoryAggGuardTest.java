package com.dev.lib.mongo;

import com.dev.lib.entity.dsl.DslQuery;
import org.junit.jupiter.api.Test;

import java.lang.invoke.MethodHandles;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BaseRepositoryAggGuardTest {

    @SuppressWarnings("unchecked")
    private final BaseRepository<TestMongoEntity> repository = (BaseRepository<TestMongoEntity>) Proxy.newProxyInstance(
            BaseRepository.class.getClassLoader(),
            new Class<?>[]{
                    BaseRepository.class
            },
            new DefaultMethodHandler()
    );

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

    static class DefaultMethodHandler implements InvocationHandler {

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
            throw new UnsupportedOperationException("unexpected call: " + method.getName());
        }
    }
}
