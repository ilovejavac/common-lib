package com.dev.lib.util.parallel;

import com.dev.lib.util.Dispatcher;
import lombok.RequiredArgsConstructor;

import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

@RequiredArgsConstructor
public final class ParallelCollection<E> {

    private final Collection<E> data;

    public void apply(Consumer<E> block) {

        CompletableFuture.allOf(data.stream()
                                        .map(item -> CompletableFuture.runAsync(
                                                () -> block.accept(item),
                                                Dispatcher.IO
                                        ))
                                        .toArray(CompletableFuture[]::new)
        ).join();
    }

}
