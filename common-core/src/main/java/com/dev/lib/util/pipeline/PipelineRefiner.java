package com.dev.lib.util.pipeline;

@FunctionalInterface
public interface PipelineRefiner<C extends PipeLineContext<O>, O> {

    void refine(C ctx);

}