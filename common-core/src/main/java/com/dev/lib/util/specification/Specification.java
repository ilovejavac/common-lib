package com.dev.lib.util.specification;

import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * 规格模式接口
 * 用于构建可组合的业务规则条件
 *
 * @param <T> 待判断的对象类型
 */
@FunctionalInterface
public interface Specification<T> {

    /**
     * 判断对象是否满足规格
     */
    boolean isSatisfiedBy(T candidate);

    /**
     * 与操作
     */
    default Specification<T> and(Specification<T> other) {

        return candidate -> this.isSatisfiedBy(candidate) && other.isSatisfiedBy(candidate);
    }

    /**
     * 或操作
     */
    default Specification<T> or(Specification<T> other) {

        return candidate -> this.isSatisfiedBy(candidate) || other.isSatisfiedBy(candidate);
    }

    /**
     * 非操作
     */
    default Specification<T> not() {

        return candidate -> !this.isSatisfiedBy(candidate);
    }

    /**
     * 转换为 Predicate
     */
    default Predicate<T> toPredicate() {

        return this::isSatisfiedBy;
    }

    /**
     * 过滤集合
     */
    default List<T> filter(Collection<T> candidates) {

        return candidates.stream().filter(this::isSatisfiedBy).toList();
    }

    /**
     * 过滤流
     */
    default Stream<T> filter(Stream<T> stream) {

        return stream.filter(this::isSatisfiedBy);
    }

    /**
     * 统计满足条件的数量
     */
    default long count(Collection<T> candidates) {

        return candidates.stream().filter(this::isSatisfiedBy).count();
    }

    /**
     * 是否存在满足条件的元素
     */
    default boolean anyMatch(Collection<T> candidates) {

        return candidates.stream().anyMatch(this::isSatisfiedBy);
    }

    /**
     * 是否所有元素都满足条件
     */
    default boolean allMatch(Collection<T> candidates) {

        return candidates.stream().allMatch(this::isSatisfiedBy);
    }

    /**
     * 是否没有元素满足条件
     */
    default boolean noneMatch(Collection<T> candidates) {

        return candidates.stream().noneMatch(this::isSatisfiedBy);
    }

    // --- 静态工厂方法 ---

    /**
     * 永远为真的规格
     */
    static <T> Specification<T> always() {

        return candidate -> true;
    }

    /**
     * 永远为假的规格
     */
    static <T> Specification<T> never() {

        return candidate -> false;
    }

    /**
     * 从 Predicate 创建
     */
    static <T> Specification<T> of(Predicate<T> predicate) {

        return predicate::test;
    }

    /**
     * 所有规格都满足
     */
    @SafeVarargs
    static <T> Specification<T> allOf(Specification<T>... specs) {

        return candidate -> {
            for (Specification<T> spec : specs) {
                if (!spec.isSatisfiedBy(candidate)) return false;
            }
            return true;
        };
    }

    /**
     * 任一规格满足
     */
    @SafeVarargs
    static <T> Specification<T> anyOf(Specification<T>... specs) {

        return candidate -> {
            for (Specification<T> spec : specs) {
                if (spec.isSatisfiedBy(candidate)) return true;
            }
            return false;
        };
    }

    /**
     * 没有规格满足
     */
    @SafeVarargs
    static <T> Specification<T> noneOf(Specification<T>... specs) {

        return anyOf(specs).not();
    }

}