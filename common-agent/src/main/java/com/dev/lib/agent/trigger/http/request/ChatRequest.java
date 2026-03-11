package com.dev.lib.agent.trigger.http.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ChatRequest {

    private String sessionId;

    @NotBlank(message = "prompt must not be blank")
    @Size(max = 10000, message = "prompt is too long")
    private String prompt;
}
