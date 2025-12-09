package com.dev.lib.util.pipeline;

public interface StageFilter <O> {
    O doFilter(O input);
}
