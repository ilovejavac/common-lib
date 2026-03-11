package com.dev.lib.agent.domain.model;

import java.time.Instant;

public record PendingUserMessage(
        String messageId,
        String prompt,
        Instant timestamp
) {

    public static PendingUserMessage of(String messageId, String prompt, Instant timestamp) {

        return new PendingUserMessage(messageId, prompt, timestamp);
    }
}
