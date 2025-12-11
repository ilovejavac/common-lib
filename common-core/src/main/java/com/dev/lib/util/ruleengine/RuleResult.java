package com.dev.lib.util.ruleengine;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * 规则校验结果
 *
 * @param <E> 错误信息类型
 */
public record RuleResult<E>(
        boolean passed,
        List<E> errors,
        List<String> passedRules
) {

    /**
     * 是否通过所有规则
     */
    public boolean isPassed() {

        return passed;
    }

    /**
     * 是否失败
     */
    public boolean isFailed() {

        return !passed;
    }

    /**
     * 获取第一个错误
     */
    public E getFirstError() {

        return errors.isEmpty() ? null : errors.get(0);
    }

    /**
     * 错误数量
     */
    public int errorCount() {

        return errors.size();
    }

    /**
     * 如果失败则执行
     */
    public RuleResult<E> ifFailed(Consumer<List<E>> action) {

        if (isFailed()) {
            action.accept(errors);
        }
        return this;
    }

    /**
     * 如果通过则执行
     */
    public RuleResult<E> ifPassed(Runnable action) {

        if (isPassed()) {
            action.run();
        }
        return this;
    }

    /**
     * 转换错误信息
     */
    public <R> RuleResult<R> mapErrors(Function<E, R> mapper) {

        List<R> mapped = errors.stream().map(mapper).toList();
        return new RuleResult<>(
                passed,
                mapped,
                passedRules
        );
    }

    /**
     * 如果失败则抛出异常
     */
    public RuleResult<E> orElseThrow(Function<List<E>, ? extends RuntimeException> exceptionSupplier) {

        if (isFailed()) {
            throw exceptionSupplier.apply(errors);
        }
        return this;
    }

}