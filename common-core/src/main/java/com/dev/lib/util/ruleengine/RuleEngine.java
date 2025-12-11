package com.dev.lib.util.ruleengine;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.impl.factory.Lists;
import org.springframework.lang.NonNull;
import org.springframework.util.Assert;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/**
 * 轻量级规则引擎
 *
 * @param <I> Input 输入类型
 * @param <E> Error 错误信息类型
 */
@Slf4j
@SuppressWarnings("all")
@RequiredArgsConstructor
public class RuleEngine<I, E> {

    private final ImmutableList<Rule<I, E>> rules;

    private final boolean                   failFast;

    private final RuleListener<I, E>        listener;

    public static <I, E> Builder<I, E> builder() {

        return new Builder<>();
    }

    /**
     * 执行规则校验
     *
     * @param input 待校验对象
     * @return 校验结果
     */
    public RuleResult<E> evaluate(@NonNull I input) {

        Assert.notNull(
                input,
                "input cannot be null"
        );

        List<E>      errors      = new ArrayList<>();
        List<String> passedRules = new ArrayList<>();

        for (Rule<I, E> rule : rules) {
            try {
                boolean passed = rule.condition().test(input);

                if (passed) {
                    passedRules.add(rule.name());
                    if (listener != null) listener.onPass(
                            rule.name(),
                            input
                    );
                } else {
                    errors.add(rule.errorMessage());
                    if (listener != null) listener.onFail(
                            rule.name(),
                            input,
                            rule.errorMessage()
                    );

                    if (failFast) {
                        log.debug(
                                "Rule [{}] failed, fail-fast enabled, stopping evaluation",
                                rule.name()
                        );
                        break;
                    }
                }
            } catch (Exception e) {
                log.error(
                        "Rule [{}] execution error: {}",
                        rule.name(),
                        e.getMessage()
                );
                if (listener != null) listener.onError(
                        rule.name(),
                        input,
                        e
                );

                if (failFast) throw new RuleExecutionException(
                        rule.name(),
                        e
                );
            }
        }

        return new RuleResult<>(
                errors.isEmpty(),
                errors,
                passedRules
        );
    }

    /**
     * 简单校验，仅返回是否通过
     */
    public boolean isValid(I input) {

        return evaluate(input).isPassed();
    }

    // --- 内部记录类 ---
    private record Rule<I, E>(String name, Predicate<I> condition, E errorMessage, int priority) {}

    // --- 监听器接口 ---
    public interface RuleListener<I, E> {

        default void onPass(String ruleName, I input) {

        }

        default void onFail(String ruleName, I input, E error) {

        }

        default void onError(String ruleName, I input, Exception e) {

        }

    }

    // --- 规则执行异常 ---
    public static class RuleExecutionException extends RuntimeException {

        public RuleExecutionException(String ruleName, Throwable cause) {

            super(
                    "Rule execution failed: " + ruleName,
                    cause
            );
        }

    }

    // --- Builder ---
    public static class Builder<I, E> {

        private final List<Rule<I, E>>   rules    = new ArrayList<>();

        private       boolean            failFast = true;

        private       RuleListener<I, E> listener;

        /**
         * 添加规则
         *
         * @param name         规则名称
         * @param condition    规则条件 (返回true表示通过)
         * @param errorMessage 不通过时的错误信息
         */
        public Builder<I, E> rule(String name, Predicate<I> condition, E errorMessage) {

            return rule(
                    name,
                    condition,
                    errorMessage,
                    0
            );
        }

        public Builder<I, E> rule(String name, Predicate<I> condition, E errorMessage, int priority) {

            rules.add(new Rule<>(
                    name,
                    condition,
                    errorMessage,
                    priority
            ));
            return this;
        }

        /**
         * 是否快速失败 (遇到第一个失败规则即停止)
         */
        public Builder<I, E> failFast(boolean failFast) {

            this.failFast = failFast;
            return this;
        }

        public Builder<I, E> listener(RuleListener<I, E> listener) {

            this.listener = listener;
            return this;
        }

        public RuleEngine<I, E> build() {
            // 按优先级排序
            List<Rule<I, E>> sorted = rules.stream()
                    .sorted((r1, r2) -> Integer.compare(
                            r2.priority(),
                            r1.priority()
                    ))
                    .toList();
            return new RuleEngine<>(
                    Lists.immutable.ofAll(sorted),
                    failFast,
                    listener
            );
        }

    }

}