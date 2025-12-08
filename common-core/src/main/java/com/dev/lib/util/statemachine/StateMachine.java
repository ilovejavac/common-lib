package com.dev.lib.util.statemachine;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.collections.api.map.ImmutableMap;
import org.eclipse.collections.impl.factory.Maps;
import org.springframework.lang.NonNull;
import org.springframework.util.Assert;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * 轻量级状态机
 *
 * @param <S> State 状态类型 (建议使用枚举)
 * @param <E> Event 事件类型 (建议使用枚举)
 * @param <C> Context 上下文对象
 */
@Slf4j
@SuppressWarnings("all")
@RequiredArgsConstructor
public class StateMachine<S, E, C> {

    private final ImmutableMap<TransitionKey<S, E>, Transition<S, E, C>> transitions;
    private final StateChangeListener<S, C> listener;

    public static <S, E, C> Builder<S, E, C> builder() {
        return new Builder<>();
    }

    /**
     * 触发状态转换
     *
     * @param currentState 当前状态
     * @param event        触发事件
     * @param context      上下文对象
     * @return 转换后的新状态
     */
    public S fire(@NonNull S currentState, @NonNull E event, C context) {
        Assert.notNull(currentState, "currentState cannot be null");
        Assert.notNull(event, "event cannot be null");

        TransitionKey<S, E> key = new TransitionKey<>(currentState, event);
        Transition<S, E, C> transition = transitions.get(key);

        if (transition == null) {
            throw new IllegalStateException(
                    String.format("No transition defined for state [%s] with event [%s]", currentState, event));
        }

        // 执行转换动作
        if (transition.action() != null) {
            transition.action().accept(context);
        }

        S newState = transition.target();

        // 触发监听器
        if (listener != null) {
            listener.onStateChange(currentState, newState, event, context);
        }

        log.debug("State transition: {} --[{}]--> {}", currentState, event, newState);
        return newState;
    }

    /**
     * 安全触发，不抛异常
     */
    public Optional<S> fireSafe(S currentState, E event, C context) {
        try {
            return Optional.of(fire(currentState, event, context));
        } catch (IllegalStateException e) {
            log.warn("Transition failed: {}", e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * 检查是否可以触发某事件
     */
    public boolean canFire(S currentState, E event) {
        return transitions.containsKey(new TransitionKey<>(currentState, event));
    }

    // --- 内部记录类 ---
    private record TransitionKey<S, E>(S source, E event) {}

    private record Transition<S, E, C>(S source, E event, S target, Consumer<C> action) {}

    // --- 监听器接口 ---
    @FunctionalInterface
    public interface StateChangeListener<S, C> {
        void onStateChange(S from, S to, Object event, C context);
    }

    // --- Builder ---
    public static class Builder<S, E, C> {
        private final Map<TransitionKey<S, E>, Transition<S, E, C>> transitions = new HashMap<>();
        private StateChangeListener<S, C> listener;

        /**
         * 定义状态转换
         *
         * @param source 源状态
         * @param event  触发事件
         * @param target 目标状态
         * @param action 转换时执行的动作 (可为null)
         */
        public Builder<S, E, C> transition(S source, E event, S target, Consumer<C> action) {
            TransitionKey<S, E> key = new TransitionKey<>(source, event);
            transitions.put(key, new Transition<>(source, event, target, action));
            return this;
        }

        public Builder<S, E, C> transition(S source, E event, S target) {
            return transition(source, event, target, null);
        }

        /**
         * 批量定义：同一事件触发多个状态的相同转换
         */
        public Builder<S, E, C> transitions(E event, S target, Consumer<C> action, S... sources) {
            for (S source : sources) {
                transition(source, event, target, action);
            }
            return this;
        }

        public Builder<S, E, C> onStateChange(StateChangeListener<S, C> listener) {
            this.listener = listener;
            return this;
        }

        public StateMachine<S, E, C> build() {
            Assert.notEmpty(transitions, "At least one transition must be defined");
            return new StateMachine<>(Maps.immutable.ofMap(transitions), listener);
        }
    }
}