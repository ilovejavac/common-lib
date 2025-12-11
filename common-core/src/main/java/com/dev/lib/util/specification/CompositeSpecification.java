package com.dev.lib.util.specification;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.impl.factory.Lists;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * 可命名的复合规格
 * 支持追踪哪些规格通过/失败
 *
 * @param <T> 待判断的对象类型
 */
@SuppressWarnings("all")
@RequiredArgsConstructor
public class CompositeSpecification<T> implements Specification<T> {

    @Getter
    private final String                      name;

    private final ImmutableList<NamedSpec<T>> specs;

    private final CompositionType             type;

    public static <T> Builder<T> builder(String name) {

        return new Builder<>(name);
    }

    @Override
    public boolean isSatisfiedBy(T candidate) {

        return switch (type) {
            case AND -> specs.allSatisfy(s -> s.spec().isSatisfiedBy(candidate));
            case OR -> specs.anySatisfy(s -> s.spec().isSatisfiedBy(candidate));
        };
    }

    /**
     * 详细评估，返回每个子规格的结果
     */
    public EvaluationResult<T> evaluate(T candidate) {

        List<SpecResult> results = new ArrayList<>();

        for (NamedSpec<T> namedSpec : specs) {
            boolean satisfied = namedSpec.spec().isSatisfiedBy(candidate);
            results.add(new SpecResult(
                    namedSpec.name(),
                    satisfied
            ));
        }

        boolean overall = switch (type) {
            case AND -> results.stream().allMatch(SpecResult::satisfied);
            case OR -> results.stream().anyMatch(SpecResult::satisfied);
        };

        return new EvaluationResult<>(
                name,
                overall,
                results,
                candidate
        );
    }

    // --- 内部类型 ---
    private record NamedSpec<T>(String name, Specification<T> spec) {}

    public record SpecResult(String name, boolean satisfied) {}

    public record EvaluationResult<T>(
            String specName,
            boolean satisfied,
            List<SpecResult> details,
            T candidate
    ) {

        public List<String> failedSpecs() {

            return details.stream()
                    .filter(r -> !r.satisfied())
                    .map(SpecResult::name)
                    .toList();
        }

        public List<String> passedSpecs() {

            return details.stream()
                    .filter(SpecResult::satisfied)
                    .map(SpecResult::name)
                    .toList();
        }

    }

    public enum CompositionType {
        AND, OR
    }

    // --- Builder ---
    public static class Builder<T> {

        private final String             name;

        private final List<NamedSpec<T>> specs = new ArrayList<>();

        private       CompositionType    type  = CompositionType.AND;

        public Builder(String name) {

            this.name = name;
        }

        public Builder<T> spec(String specName, Specification<T> spec) {

            specs.add(new NamedSpec<>(
                    specName,
                    spec
            ));
            return this;
        }

        /**
         * 基于属性的规格
         */
        public <V> Builder<T> property(String specName, Function<T, V> extractor, Specification<V> spec) {

            specs.add(new NamedSpec<>(
                    specName,
                    t -> spec.isSatisfiedBy(extractor.apply(t))
            ));
            return this;
        }

        public Builder<T> and() {

            this.type = CompositionType.AND;
            return this;
        }

        public Builder<T> or() {

            this.type = CompositionType.OR;
            return this;
        }

        public CompositeSpecification<T> build() {

            return new CompositeSpecification<>(
                    name,
                    Lists.immutable.ofAll(specs),
                    type
            );
        }

    }

}