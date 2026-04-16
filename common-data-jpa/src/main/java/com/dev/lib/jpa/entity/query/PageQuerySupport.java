package com.dev.lib.jpa.entity.query;

import com.querydsl.core.Tuple;
import com.querydsl.core.types.Expression;
import com.querydsl.core.types.dsl.Expressions;

import java.util.List;
import java.util.Locale;
import java.util.function.LongSupplier;

public final class PageQuerySupport {

    static final Expression<Long> WINDOW_TOTAL_EXPRESSION = Expressions.numberTemplate(
            Long.class,
            "count(*) over()"
    ).as("_total_");

    private PageQuerySupport() {
    }

    public static long resolveWindowTotal(List<Tuple> tuples, Expression<Long> totalExpression, LongSupplier countFallback) {

        if (tuples == null || tuples.isEmpty()) {
            return countFallback.getAsLong();
        }

        Long total = tuples.getFirst().get(totalExpression);
        if (total != null) {
            return total;
        }
        // 数据库不支持 count(*) over()，fallback 到 count 查询
        return countFallback.getAsLong();
    }

    /**
     * 全字段查询时，Hibernate 展开实体列后 Expression 引用匹配不上，
     * 改用 Tuple 最后一列（即 count(*) over()）取值。
     */
    public static long resolveWindowTotalFromTuples(List<Tuple> tuples, LongSupplier countFallback) {

        if (tuples == null || tuples.isEmpty()) {
            return countFallback.getAsLong();
        }

        Object[] array = tuples.getFirst().toArray();
        Object last = array[array.length - 1];
        if (last instanceof Number num) {
            return num.longValue();
        }
        // 数据库不支持 count(*) over()，fallback 到 count 查询
        return countFallback.getAsLong();
    }

    public static boolean shouldFallbackToLegacyPage(Throwable ex) {

        for (Throwable current = ex; current != null; current = current.getCause()) {
            String message = current.getMessage();
            if (message == null) {
                continue;
            }
            String normalized = message.toLowerCase(Locale.ROOT);
            if (normalized.contains("window")
                    || normalized.contains("over(")
                    || normalized.contains("count(*) over()")
                    || normalized.contains("syntax")
                    || normalized.contains("parse")) {
                return true;
            }
        }
        return false;
    }
}
