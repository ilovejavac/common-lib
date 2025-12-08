package com.dev.lib.util;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Supplier;

public class AsyncAggregator {

    public static <T1, T2, R> CompletableFuture<R> zip(
            CompletableFuture<T1> f1,
            CompletableFuture<T2> f2,
            java.util.function.BiFunction<? super T1, ? super T2, ? extends R> combiner
    ) {
        return f1.thenCombine(f2, combiner);
    }

    // 针对业务场景的 Tuple 包装，避免手动写 thenCombine
    public static <T1, T2> Tuple2Future<T1, T2> with(Supplier<T1> task1, Supplier<T2> task2, Executor executor) {
        return new Tuple2Future<>(
                CompletableFuture.supplyAsync(task1, executor),
                CompletableFuture.supplyAsync(task2, executor)
        );
    }

    @AllArgsConstructor
    public static class Tuple2Future<T1, T2> {
        private final CompletableFuture<T1> f1;
        private final CompletableFuture<T2> f2;

        public <R> CompletableFuture<R> map(java.util.function.BiFunction<T1, T2, R> mapper) {
            return f1.thenCombine(f2, mapper);
        }
    }

    public static <T1, T2, T3> Tuple3Future<T1, T2, T3> with(
            Supplier<T1> t1, Supplier<T2> t2, Supplier<T3> t3, Executor ex) {
        return new Tuple3Future<>(
                CompletableFuture.supplyAsync(t1, ex),
                CompletableFuture.supplyAsync(t2, ex),
                CompletableFuture.supplyAsync(t3, ex)
        );
    }
    public static <T1, T2, T3, T4> Tuple4Future<T1, T2, T3, T4> with(
            Supplier<T1> t1, Supplier<T2> t2, Supplier<T3> t3, Supplier<T4> t4, Executor ex) {
        return new Tuple4Future<>(
                CompletableFuture.supplyAsync(t1, ex),
                CompletableFuture.supplyAsync(t2, ex),
                CompletableFuture.supplyAsync(t3, ex),
                CompletableFuture.supplyAsync(t4, ex)
        );
    }
    public static <T1, T2, T3, T4, T5> Tuple5Future<T1, T2, T3, T4, T5> with(
            Supplier<T1> t1, Supplier<T2> t2, Supplier<T3> t3, Supplier<T4> t4, Supplier<T5> t5, Executor ex) {
        return new Tuple5Future<>(
                CompletableFuture.supplyAsync(t1, ex),
                CompletableFuture.supplyAsync(t2, ex),
                CompletableFuture.supplyAsync(t3, ex),
                CompletableFuture.supplyAsync(t4, ex),
                CompletableFuture.supplyAsync(t5, ex)
        );
    }
    // --- Tuple Futures ---
    @AllArgsConstructor
    public static class Tuple3Future<T1, T2, T3> {
        private final CompletableFuture<T1> f1;
        private final CompletableFuture<T2> f2;
        private final CompletableFuture<T3> f3;
        public <R> CompletableFuture<R> map(Function3<T1, T2, T3, R> mapper) {
            return CompletableFuture.allOf(f1, f2, f3)
                    .thenApply(v -> mapper.apply(f1.join(), f2.join(), f3.join()));
        }
    }
    @AllArgsConstructor
    public static class Tuple4Future<T1, T2, T3, T4> {
        private final CompletableFuture<T1> f1;
        private final CompletableFuture<T2> f2;
        private final CompletableFuture<T3> f3;
        private final CompletableFuture<T4> f4;
        public <R> CompletableFuture<R> map(Function4<T1, T2, T3, T4, R> mapper) {
            return CompletableFuture.allOf(f1, f2, f3, f4)
                    .thenApply(v -> mapper.apply(f1.join(), f2.join(), f3.join(), f4.join()));
        }
    }
    @AllArgsConstructor
    public static class Tuple5Future<T1, T2, T3, T4, T5> {
        private final CompletableFuture<T1> f1;
        private final CompletableFuture<T2> f2;
        private final CompletableFuture<T3> f3;
        private final CompletableFuture<T4> f4;
        private final CompletableFuture<T5> f5;
        public <R> CompletableFuture<R> map(Function5<T1, T2, T3, T4, T5, R> mapper) {
            return CompletableFuture.allOf(f1, f2, f3, f4, f5)
                    .thenApply(v -> mapper.apply(f1.join(), f2.join(), f3.join(), f4.join(), f5.join()));
        }
    }
    // --- 自定义函数式接口 (JDK没有提供3个参数以上的Function) ---
    @FunctionalInterface
    public interface Function3<T1, T2, T3, R> {
        R apply(T1 t1, T2 t2, T3 t3);
    }
    @FunctionalInterface
    public interface Function4<T1, T2, T3, T4, R> {
        R apply(T1 t1, T2 t2, T3 t3, T4 t4);
    }
    @FunctionalInterface
    public interface Function5<T1, T2, T3, T4, T5, R> {
        R apply(T1 t1, T2 t2, T3 t3, T4 t4, T5 t5);
    }
}
