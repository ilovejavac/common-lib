package com.dev.lib.jpa.entity;

import com.querydsl.core.Tuple;
import com.querydsl.core.types.Expression;
import com.querydsl.core.types.dsl.Expressions;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.LongSupplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BaseRepositoryImplWindowCountTest {

    @Test
    void shouldUseWindowTotalWhenPresent() {

        Expression<Long> totalExpression = Expressions.numberTemplate(Long.class, "count(*) over()");
        Tuple tuple = mock(Tuple.class);
        when(tuple.get(totalExpression)).thenReturn(23L);

        AtomicInteger fallbackCalls = new AtomicInteger();
        LongSupplier fallback = () -> {
            fallbackCalls.incrementAndGet();
            return 0L;
        };

        long total = BaseRepositoryImpl.resolveWindowTotal(List.of(tuple), totalExpression, fallback);

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

        long total = BaseRepositoryImpl.resolveWindowTotal(List.of(), totalExpression, fallback);

        assertThat(total).isEqualTo(7L);
        assertThat(fallbackCalls.get()).isEqualTo(1);
    }

    @Test
    void shouldFallbackWhenWindowTotalIsNull() {

        Expression<Long> totalExpression = Expressions.numberTemplate(Long.class, "count(*) over()");
        Tuple tuple = mock(Tuple.class);
        when(tuple.get(totalExpression)).thenReturn(null);

        AtomicInteger fallbackCalls = new AtomicInteger();
        LongSupplier fallback = () -> {
            fallbackCalls.incrementAndGet();
            return 11L;
        };

        long total = BaseRepositoryImpl.resolveWindowTotal(List.of(tuple), totalExpression, fallback);

        assertThat(total).isEqualTo(11L);
        assertThat(fallbackCalls.get()).isEqualTo(1);
    }

    @Test
    void shouldFallbackForWindowSyntaxErrors() {

        RuntimeException ex = new RuntimeException("You have an error in your SQL syntax near 'over()'");

        assertThat(BaseRepositoryImpl.shouldFallbackToLegacyPage(ex)).isTrue();
    }

    @Test
    void shouldNotFallbackForUnrelatedErrors() {

        RuntimeException ex = new RuntimeException("Null pointer while mapping dto");

        assertThat(BaseRepositoryImpl.shouldFallbackToLegacyPage(ex)).isFalse();
    }
}
