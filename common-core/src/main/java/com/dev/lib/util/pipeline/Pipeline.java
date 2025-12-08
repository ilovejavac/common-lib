package com.dev.lib.util.pipeline;

import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.impl.factory.Lists;
import org.springframework.lang.NonNull;
import org.springframework.util.Assert;

import java.util.List;
import java.util.Optional;
import java.util.function.BiPredicate;

@Slf4j
@SuppressWarnings("all")
@RequiredArgsConstructor
public class Pipeline<I, C extends PipeLineContext<O>, O> {
    private final ImmutableList<Stage<I, C, O>> stageList;
    @Setter
    private O defaultResult;

    // 1. 拦截器接口 (Stage, Context) -> void
    private final PipelineInterceptor<I, C, O> interceptor;
    // 2. 异常处理器 (Throwable, Context) -> Boolean (返回 true 表示忽略异常继续执行，false 表示终止)
    private final BiPredicate<Throwable, C> errorHandler;

    public static <I, C extends PipeLineContext<O>, O> Builder<I, C, O> builder() {
        return new Builder<>();
    }

    public final O execute(@NonNull I input, @NonNull C ctx) {
        Assert.notNull(input, "input cannot be null");
        Assert.notNull(ctx, "ctx cannot be null");

        for (Stage<I, C, O> stage : stageList) {
            try {
                // A. 前置拦截
                if (interceptor != null) interceptor.before(stage, input, ctx);

                // B. 执行核心逻辑
                stage.execute(input, ctx);

                // C. 后置拦截
                if (interceptor != null) interceptor.after(stage, input, ctx);

            } catch (Exception e) {
                // D. 容错处理
                if (interceptor != null) interceptor.exception(stage, e, ctx);

                // 如果没有配置 errorHandler，默认抛出异常 (Fail-Fast)
                if (errorHandler == null) throw e;

                // 如果 errorHandler 返回 false，则中断链条；返回 true 则忽略异常继续下一个 Stage
                boolean shouldContinue = errorHandler.test(e, ctx);
                if (!shouldContinue) break;
            }

            if (ctx.isTerminated()) break;
        }
        return Optional.ofNullable(ctx.getOutput()).orElse(defaultResult);
    }

    // --- 定义简单的拦截接口 ---
    public interface PipelineInterceptor<I, C extends PipeLineContext<O>, O> {
        default void before(Stage<I, C, O> stage, I input, C ctx) {
        }

        default void after(Stage<I, C, O> stage, I input, C ctx) {
        }

        default void exception(Stage<I, C, O> stage, Exception e, C ctx) {
        }
    }

    // --- Builder ---
    public static class Builder<I, C extends PipeLineContext<O>, O> {
        private final List<Stage<I, C, O>> stageList = Lists.mutable.empty();
        private O defaultResult;
        private PipelineInterceptor<I, C, O> interceptor;
        private BiPredicate<Throwable, C> errorHandler;

        public Builder<I, C, O> add(Stage<I, C, O> stage) {
            stageList.add(stage);
            return this;
        }

        public Builder<I, C, O> defaultResult(O result) {
            this.defaultResult = result;
            return this;
        }

        // 注入拦截器
        public Builder<I, C, O> interceptor(PipelineInterceptor<I, C, O> interceptor) {
            this.interceptor = interceptor;
            return this;
        }

        // 注入异常处理策略
        public Builder<I, C, O> errorHandler(BiPredicate<Throwable, C> errorHandler) {
            this.errorHandler = errorHandler;
            return this;
        }

        public Pipeline<I, C, O> build() {
            return new Pipeline<>(Lists.immutable.ofAll(stageList), interceptor, errorHandler)
                    .setDefaultResult(defaultResult);
        }
    }
}
