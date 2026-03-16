package com.dev.lib.harness.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "app.agent")
public class AgentProperties {

    private boolean enable = true;

    private Integer maxPendingUserMessage = 16;

    private Integer maxCompressThreshold = 256 * 1024;


}
