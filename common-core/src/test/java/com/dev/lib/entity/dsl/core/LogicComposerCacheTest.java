package com.dev.lib.entity.dsl.core;

import com.dev.lib.entity.CoreEntity;
import com.dev.lib.entity.dsl.DslQuery;
import com.dev.lib.entity.dsl.QueryRef;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LogicComposerCacheTest {

    @BeforeEach
    void setUp() {

        LambdaFieldNameResolver.clearCacheForTest();
        LogicComposer.clearCacheForTest();
    }

    @Test
    void shouldCacheGetterFieldNameResolution() {

        QueryRef<ArrangeQuery, String> c1 = ArrangeQuery::getC1;
        QueryRef<ArrangeQuery, String> c2 = ArrangeQuery::getC2;

        for (int i = 0; i < 10; i++) {
            assertThat(c1.getFieldName()).isEqualTo("c1");
            assertThat(c2.getFieldName()).isEqualTo("c2");
        }

        assertThat(LambdaFieldNameResolver.fieldNameCacheSizeForTest()).isEqualTo(2);
        assertThat(LambdaFieldNameResolver.lambdaCacheSizeForTest()).isLessThanOrEqualTo(2);
    }

    @Test
    void shouldCachePostfixCompilationByLogicTokens() {

        ArrangeQuery query1 = new ArrangeQuery();
        query1.where().use(ArrangeQuery::getC1)
                .orBegin()
                .use(ArrangeQuery::getC2)
                .and(ArrangeQuery::getC3)
                .end();

        String expr1 = LogicComposer.compose(
                query1.where().logicTokens(),
                field -> field,
                new StringCombiner()
        );

        assertThat(expr1).isEqualTo("c1 OR c2 AND c3");
        assertThat(LogicComposer.cacheSizeForTest()).isEqualTo(1);

        ArrangeQuery query2 = new ArrangeQuery();
        query2.where().use(ArrangeQuery::getC1)
                .orBegin()
                .use(ArrangeQuery::getC2)
                .and(ArrangeQuery::getC3)
                .end();

        String expr2 = LogicComposer.compose(
                query2.where().logicTokens(),
                field -> field,
                new StringCombiner()
        );

        assertThat(expr2).isEqualTo(expr1);
        assertThat(LogicComposer.cacheSizeForTest()).isEqualTo(1);
    }

    static class StringCombiner implements LogicComposer.Combiner<String> {

        @Override
        public String and(String left, String right) {

            return left + " AND " + right;
        }

        @Override
        public String or(String left, String right) {

            return left + " OR " + right;
        }
    }

    static class ArrangeQuery extends DslQuery<CoreEntity> {

        private String c1;

        private String c2;

        private String c3;

        public String getC1() {

            return c1;
        }

        public String getC2() {

            return c2;
        }

        public String getC3() {

            return c3;
        }
    }
}
