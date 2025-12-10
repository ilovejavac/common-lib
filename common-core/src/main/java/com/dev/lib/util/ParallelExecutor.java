package com.dev.lib.util;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Supplier;

public abstract class ParallelExecutor {

    public static <T1, T2> Tuple2<T1, T2> with(Supplier<T1> task1, Supplier<T2> task2) {
        return with(task1, task2, Dispatcher.IO);
    }

    public static <T1, T2> Tuple2<T1, T2> with(Supplier<T1> task1, Supplier<T2> task2, Executor executor) {
        CompletableFuture<T1> f1 = CompletableFuture.supplyAsync(task1, executor);
        CompletableFuture<T2> f2 = CompletableFuture.supplyAsync(task2, executor);
        return new Tuple2<>(f1.join(), f2.join());
    }

    public static <T1, T2, T3> Tuple3<T1, T2, T3> with(Supplier<T1> t1, Supplier<T2> t2, Supplier<T3> t3) {
        return with(t1, t2, t3, Dispatcher.IO);
    }

    public static <T1, T2, T3> Tuple3<T1, T2, T3> with(Supplier<T1> t1, Supplier<T2> t2, Supplier<T3> t3, Executor ex) {
        CompletableFuture<T1> f1 = CompletableFuture.supplyAsync(t1, ex);
        CompletableFuture<T2> f2 = CompletableFuture.supplyAsync(t2, ex);
        CompletableFuture<T3> f3 = CompletableFuture.supplyAsync(t3, ex);
        return new Tuple3<>(f1.join(), f2.join(), f3.join());
    }

    public static <T1, T2, T3, T4> Tuple4<T1, T2, T3, T4> with(
            Supplier<T1> t1, Supplier<T2> t2, Supplier<T3> t3, Supplier<T4> t4) {
        return with(t1, t2, t3, t4, Dispatcher.IO);
    }

    public static <T1, T2, T3, T4> Tuple4<T1, T2, T3, T4> with(
            Supplier<T1> t1, Supplier<T2> t2, Supplier<T3> t3, Supplier<T4> t4, Executor ex) {
        CompletableFuture<T1> f1 = CompletableFuture.supplyAsync(t1, ex);
        CompletableFuture<T2> f2 = CompletableFuture.supplyAsync(t2, ex);
        CompletableFuture<T3> f3 = CompletableFuture.supplyAsync(t3, ex);
        CompletableFuture<T4> f4 = CompletableFuture.supplyAsync(t4, ex);
        return new Tuple4<>(f1.join(), f2.join(), f3.join(), f4.join());
    }

    public static <T1, T2, T3, T4, T5> Tuple5<T1, T2, T3, T4, T5> with(
            Supplier<T1> t1, Supplier<T2> t2, Supplier<T3> t3, Supplier<T4> t4, Supplier<T5> t5) {
        return with(t1, t2, t3, t4, t5, Dispatcher.IO);
    }

    public static <T1, T2, T3, T4, T5> Tuple5<T1, T2, T3, T4, T5> with(
            Supplier<T1> t1, Supplier<T2> t2, Supplier<T3> t3, Supplier<T4> t4, Supplier<T5> t5, Executor ex) {
        CompletableFuture<T1> f1 = CompletableFuture.supplyAsync(t1, ex);
        CompletableFuture<T2> f2 = CompletableFuture.supplyAsync(t2, ex);
        CompletableFuture<T3> f3 = CompletableFuture.supplyAsync(t3, ex);
        CompletableFuture<T4> f4 = CompletableFuture.supplyAsync(t4, ex);
        CompletableFuture<T5> f5 = CompletableFuture.supplyAsync(t5, ex);
        return new Tuple5<>(f1.join(), f2.join(), f3.join(), f4.join(), f5.join());
    }

    @Getter
    @AllArgsConstructor
    public static class Tuple2<T1, T2> {
        private final T1 value1;
        private final T2 value2;
    }

    @Getter
    @AllArgsConstructor
    public static class Tuple3<T1, T2, T3> {
        private final T1 value1;
        private final T2 value2;
        private final T3 value3;
    }

    @Getter
    @AllArgsConstructor
    public static class Tuple4<T1, T2, T3, T4> {
        private final T1 value1;
        private final T2 value2;
        private final T3 value3;
        private final T4 value4;
    }

    @Getter
    @AllArgsConstructor
    public static class Tuple5<T1, T2, T3, T4, T5> {
        private final T1 value1;
        private final T2 value2;
        private final T3 value3;
        private final T4 value4;
        private final T5 value5;
    }
}
