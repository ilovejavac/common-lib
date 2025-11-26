package com.dev.lib.web.model;

@FunctionalInterface
public interface Convert<S, T> {
    T convert(S source);
}
