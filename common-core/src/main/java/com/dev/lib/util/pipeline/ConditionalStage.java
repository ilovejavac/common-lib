package com.dev.lib.util.pipeline;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/**
 * 条件分支 Stage，根据 ctx 状态路由到不同的子 Stage。
 *
 * <pre>
 * // 有 otherwise
 * ConditionalStage.when(ctx -> ctx.isX(), cNode)
 *                 .when(ctx -> ctx.isY(), dNode)
 *                 .otherwise(defaultNode)
 *
 * // 无 otherwise，Builder 本身即 PipelineStage
 * ConditionalStage.when(ctx -> ctx.isX(), cNode)
 *                 .when(ctx -> ctx.isY(), dNode)
 * </pre>
 */
public class ConditionalStage<I, C extends PipeLineContext<O>, O> implements PipelineStage<I, C, O> {

    private final List<Branch<I, C, O>> branches;

    private final PipelineStage<I, C, O> otherwise;

    private ConditionalStage(List<Branch<I, C, O>> branches, PipelineStage<I, C, O> otherwise) {

        this.branches = branches;
        this.otherwise = otherwise;
    }

    @Override
    public void execute(I input, C ctx) {

        for (Branch<I, C, O> branch : branches) {
            if (branch.condition != null && branch.condition().test(ctx)) {
                branch.stage().execute(input, ctx);
                return;
            }
        }
        if (otherwise != null) {
            otherwise.execute(input, ctx);
        }
    }

    public static <I, C extends PipeLineContext<O>, O> Builder<I, C, O> when(
            Predicate<C> condition, PipelineStage<I, C, O> stage) {

        return new Builder<I, C, O>().when(condition, stage);
    }

    // --- Builder（本身即 PipelineStage）---

    public static class Builder<I, C extends PipeLineContext<O>, O> implements PipelineStage<I, C, O> {

        private final List<Branch<I, C, O>> branches = new ArrayList<>();

        public Builder<I, C, O> when(Predicate<C> condition, PipelineStage<I, C, O> stage) {

            branches.add(new Branch<>(condition, stage));
            return this;
        }

        public ConditionalStage<I, C, O> otherwise(PipelineStage<I, C, O> stage) {

            return new ConditionalStage<>(branches, stage);
        }

        @Override
        public void execute(I input, C ctx) {

            new ConditionalStage<>(branches, null).execute(input, ctx);
        }

    }

    // --- Internal ---

    private record Branch<I, C extends PipeLineContext<O>, O>(
            Predicate<C> condition,
            PipelineStage<I, C, O> stage
    ) {}

}
