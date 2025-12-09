package com.dev.lib.util.pipeline;

public interface StageFilter<C extends PipeLineContext<O>, O> {
    O doFilter(C ctx);
}
