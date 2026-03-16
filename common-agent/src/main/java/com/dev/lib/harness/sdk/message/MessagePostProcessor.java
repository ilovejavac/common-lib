package com.dev.lib.harness.sdk.message;

public interface MessagePostProcessor {

    void postMessageOnStreaming(MessageChunk chunk);

}
