package com.dev.lib.util.pipeline;

import lombok.Data;

@Data
public abstract class PipeLineContext<O> {

    private O       output;

    private boolean terminated = false;

    public final void terminate() {

        this.terminated = true;
    }

}
