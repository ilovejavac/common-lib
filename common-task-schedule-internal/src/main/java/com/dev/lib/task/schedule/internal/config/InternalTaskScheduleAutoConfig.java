package com.dev.lib.task.schedule.internal.config;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
@ComponentScan("com.dev.lib.task.schedule.internal")
public class InternalTaskScheduleAutoConfig {
}
