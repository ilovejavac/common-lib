package com.dev.lib.util.strategy;

public interface Processor<I, O> {

    O process(I input);

}