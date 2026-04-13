package com.dev.lib.util.strategy;

import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.map.ImmutableMap;

import java.util.*;

@SuppressWarnings("all")
@RequiredArgsConstructor
public class Strategies<K, I, O> {

    private final ImmutableMap<K, Processor<I, O>> processors;

    @Setter
    private       Processor<I, O>                  defaultProcessor;

    public static <K, I, O> Builder<K, I, O> builder() {

        return new Builder<>();
    }

    /**
     * 执行单个策略
     */
    public Optional<O> execute(K key, I input) {

        Processor<I, O> processor = processors.getOrDefault(
                key,
                defaultProcessor
        );
        return processor == null ? Optional.empty() : Optional.ofNullable(processor.process(input));
    }

    public static class Builder<K, I, O> {

        private final Map<K, Processor<I, O>> map = new HashMap<>();

        private       Processor<I, O>         defaultProcessor;

        public Builder<K, I, O> register(K key, Processor<I, O> processor) {

            map.put(
                    key,
                    processor
            );
            return this;
        }

        // 支持批量注册 (例如从 Spring Map 注入)
        public Builder<K, I, O> registerAll(Map<K, Processor<I, O>> processors) {

            if (processors != null) map.putAll(processors);
            return this;
        }

        public Builder<K, I, O> setDefault(Processor<I, O> processor) {

            this.defaultProcessor = processor;
            return this;
        }

        public Strategies<K, I, O> build() {

            return new Strategies<>(Maps.immutable.ofMap(map)).setDefaultProcessor(defaultProcessor);
        }

    }

}