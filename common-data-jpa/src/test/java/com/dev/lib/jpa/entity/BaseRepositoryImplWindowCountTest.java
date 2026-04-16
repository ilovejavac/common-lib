package com.dev.lib.jpa.entity;

import com.dev.lib.jpa.entity.query.PageQuerySupport;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.Expression;
import com.querydsl.core.types.dsl.Expressions;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.LongSupplier;

import static org.assertj.core.api.Assertions.assertThat;

class PageQuerySupportWindowCountTest {

    @Test
    void shouldUseWindowTotalWhenPresent() {

        Expression<Long> totalExpression = Expressions.numberTemplate(Long.class, "count(*) over()");
        Tuple tuple = new TotalTuple(totalExpression, 23L);

        AtomicInteger fallbackCalls = new AtomicInteger();
        LongSupplier fallback = () -> {
            fallbackCalls.incrementAndGet();
            return 0L;
        };

        long total = PageQuerySupport.resolveWindowTotal(List.of(tuple), totalExpression, fallback);

        assertThat(total).isEqualTo(23L);
        assertThat(fallbackCalls.get()).isZero();
    }

    @Test
    void shouldFallbackWhenTupleIsEmpty() {

        Expression<Long> totalExpression = Expressions.numberTemplate(Long.class, "count(*) over()");
        AtomicInteger fallbackCalls = new AtomicInteger();
        LongSupplier fallback = () -> {
            fallbackCalls.incrementAndGet();
            return 7L;
        };

        long total = PageQuerySupport.resolveWindowTotal(List.of(), totalExpression, fallback);

        assertThat(total).isEqualTo(7L);
        assertThat(fallbackCalls.get()).isEqualTo(1);
    }

    @Test
    void shouldFallbackWhenFullEntityTupleIsEmpty() {

        AtomicInteger fallbackCalls = new AtomicInteger();
        LongSupplier fallback = () -> {
            fallbackCalls.incrementAndGet();
            return 13L;
        };

        long total = PageQuerySupport.resolveWindowTotalFromTuples(List.of(), fallback);

        assertThat(total).isEqualTo(13L);
        assertThat(fallbackCalls.get()).isEqualTo(1);
    }

    @Test
    void shouldFallbackWhenWindowTotalIsNull() {

        Expression<Long> totalExpression = Expressions.numberTemplate(Long.class, "count(*) over()");
        Tuple tuple = new TotalTuple(totalExpression, null);

        AtomicInteger fallbackCalls = new AtomicInteger();
        LongSupplier fallback = () -> {
            fallbackCalls.incrementAndGet();
            return 11L;
        };

        long total = PageQuerySupport.resolveWindowTotal(List.of(tuple), totalExpression, fallback);

        assertThat(total).isEqualTo(11L);
        assertThat(fallbackCalls.get()).isEqualTo(1);
    }

    @Test
    void shouldFallbackForWindowSyntaxErrors() {

        RuntimeException ex = new RuntimeException("You have an error in your SQL syntax near 'over()'");

        assertThat(PageQuerySupport.shouldFallbackToLegacyPage(ex)).isTrue();
    }

    @Test
    void shouldNotFallbackForUnrelatedErrors() {

        RuntimeException ex = new RuntimeException("Null pointer while mapping dto");

        assertThat(PageQuerySupport.shouldFallbackToLegacyPage(ex)).isFalse();
    }

    private record TotalTuple(Expression<Long> expression, Long total) implements Tuple {

        @Override
        public <T> T get(int index, Class<T> type) {

            return index == 0 && total != null ? type.cast(total) : null;
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T> T get(Expression<T> expr) {

            return expression.equals(expr) ? (T) total : null;
        }

        @Override
        public int size() {

            return 1;
        }

        @Override
        public Object[] toArray() {

            return new Object[]{total};
        }
    }
}
