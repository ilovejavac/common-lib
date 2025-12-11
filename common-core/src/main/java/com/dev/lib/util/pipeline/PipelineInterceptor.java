package com.dev.lib.util.pipeline;

public interface PipelineInterceptor<I, C extends PipeLineContext<O>, O> {

    default void before(PipelineStage<I, C, O> stage, I input, C ctx) {

    }

    default void after(PipelineStage<I, C, O> stage, I input, C ctx) {

    }

    default void onException(PipelineStage<I, C, O> stage, Exception e, C ctx) {

    }

}