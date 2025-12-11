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
    public O execute(K key, I input) {

        Processor<I, O> processor = processors.get(key);
        if (processor == null) {
            processor = defaultProcessor;
        }

        if (processor == null) {
            throw new IllegalArgumentException("No strategy found for key: " + key);
        }

        return processor.process(input);
    }

    /**
     * 【增强】安全执行：如果找不到策略，返回 Optional.empty() 而不是抛异常
     */
    public Optional<O> executeSafe(K key, I input) {

        Processor<I, O> processor = processors.getOrDefault(
                key,
                defaultProcessor
        );
        return processor == null ? Optional.empty() : Optional.ofNullable(processor.process(input));
    }

    /**
     * 【增强】批量执行：传入一组 Key，返回一组结果 (适合批量校验)
     */
    public List<O> executeGroup(List<K> keys, I input) {

        if (keys == null || keys.isEmpty()) return new ArrayList<>();
        return keys.stream()
                .map(key -> executeSafe(
                        key,
                        input
                ))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .toList();
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