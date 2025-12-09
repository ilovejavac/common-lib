package com.dev.lib.util.pipeline;

@FunctionalInterface
public interface PipelineStage<I, C extends PipeLineContext<O>, O> {
    void execute(I input, C ctx);
}