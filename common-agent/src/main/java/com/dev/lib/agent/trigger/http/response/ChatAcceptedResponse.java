package com.dev.lib.agent.trigger.http.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ChatAcceptedResponse {

    private final String sessionId;
    private final String messageId;
    private final boolean accepted;
}
