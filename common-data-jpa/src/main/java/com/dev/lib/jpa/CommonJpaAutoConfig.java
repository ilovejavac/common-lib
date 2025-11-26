package com.dev.lib.jpa;

import com.dev.lib.config.properties.AppSqlMonitorProperties;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration
@EntityScan("com.dev.lib.jpa")
@EnableJpaRepositories(basePackages = "com.dev.lib.jpa")
public class CommonJpaAutoConfig {

}
