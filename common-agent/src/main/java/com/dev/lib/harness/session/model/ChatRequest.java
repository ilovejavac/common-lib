package com.dev.lib.harness.session.model;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ChatRequest {

    private ChatEnv env;

    @NotBlank
    private String model;

    @NotBlank
    private String prompt;

}
