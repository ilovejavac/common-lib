package com.dev.lib.harness.sdk.message;

import lombok.Data;

import java.time.Instant;

@Data
public class MessageChunk {

    private String messageId;

    private String sessionId;

    private MessageRole role;

    private MessageType type;

    private String content;

    private Instant timestam;

}
