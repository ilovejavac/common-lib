package com.dev.lib.harness.session.model;

import lombok.Data;

import java.util.List;

@Data
public class ChatEnv {

    private String userPrompt;

    private List<String> files;

    private List<String> mcp;

    private List<String> knowledge;

}
