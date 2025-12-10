package com.dev.lib.util.pipeline;

import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.ImmutableList;
import org.springframework.lang.NonNull;
import org.springframework.util.Assert;

import java.util.List;
import java.util.Optional;
import java.util.function.BiPredicate;

@Slf4j
@SuppressWarnings("all")
@RequiredArgsConstructor
public class Pipeline<I, C extends PipeLineContext<O>, O> {
    private final ImmutableList<PipelineStage<I, C, O>> stages;
    private final ImmutableList<PipelineRefiner<C, O>> refiners;
    private final PipelineInterceptor<I, C, O> interceptor;
    private final BiPredicate<Throwable, C> errorHandler;
    @Setter
    private O defaultResult;

    public static <I, C extends PipeLineContext<O>, O> Builder<I, C, O> builder() {
        return new Builder<>();
    }

    public final O execute(@NonNull I input, @NonNull C ctx) {
        Assert.notNull(input, "input cannot be null");
        Assert.notNull(ctx, "ctx cannot be null");

        for (PipelineStage<I, C, O> stage : stages) {
            try {
                if (interceptor != null) interceptor.before(stage, input, ctx);
                stage.execute(input, ctx);
                if (interceptor != null) interceptor.after(stage, input, ctx);
            } catch (Exception e) {
                if (interceptor != null) interceptor.onException(stage, e, ctx);
                if (errorHandler == null) throw e;
                if (!errorHandler.test(e, ctx)) break;
            }
            if (ctx.isTerminated()) break;
        }

        for (PipelineRefiner<C, O> refiner : refiners) {
            try {
                refiner.refine(ctx);
            } catch (Exception e) {
                if (errorHandler == null) throw e;
                if (!errorHandler.test(e, ctx)) break;
            }
        }

        return Optional.ofNullable(ctx.getOutput()).orElse(defaultResult);
    }

    // --- Builder ---
    public static class Builder<I, C extends PipeLineContext<O>, O> {
        private final List<PipelineStage<I, C, O>> stages = Lists.mutable.empty();
        private final List<PipelineRefiner<C, O>> refiners = Lists.mutable.empty();
        private O defaultResult;
        private PipelineInterceptor<I, C, O> interceptor;
        private BiPredicate<Throwable, C> errorHandler;

        public Builder<I, C, O> stage(PipelineStage<I, C, O> stage) {
            stages.add(stage);
            return this;
        }

        public Builder<I, C, O> refine(PipelineRefiner<C, O> refiner) {
            refiners.add(refiner);
            return this;
        }

        public Builder<I, C, O> orElse(O result) {
            Assert.notNull(result, "pipline default-result cannot be null");
            this.defaultResult = result;
            return this;
        }

        public Builder<I, C, O> interceptor(PipelineInterceptor<I, C, O> interceptor) {
            this.interceptor = interceptor;
            return this;
        }

        public Builder<I, C, O> errorHandler(BiPredicate<Throwable, C> errorHandler) {
            this.errorHandler = errorHandler;
            return this;
        }

        public Pipeline<I, C, O> build() {
            Pipeline<I, C, O> p = new Pipeline<>(
                    Lists.immutable.ofAll(stages),
                    Lists.immutable.ofAll(refiners),
                    interceptor,
                    errorHandler
            );
            p.setDefaultResult(defaultResult);
            return p;
        }
    }
}
