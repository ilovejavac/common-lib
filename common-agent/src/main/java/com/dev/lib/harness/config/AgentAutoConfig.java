package com.dev.lib.harness.config;

import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ComponentScan("com.dev.lib.harness")
@ConfigurationPropertiesScan
public class AgentAutoConfig {
}
