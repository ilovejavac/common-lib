package com.dev.lib.config;

import io.github.linpeilie.annotations.ComponentModelConfig;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@AutoConfiguration
@ComponentScan("com.dev.lib")
@ComponentModelConfig
@EnableAsync
@EnableScheduling
public class CommonAutoConfiguration {

}