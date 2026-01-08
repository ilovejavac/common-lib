package com.dev.lib.bash;

@FunctionalInterface
public interface CommandHandler<T> {

    Object handle(T ctx, Object... args);

}