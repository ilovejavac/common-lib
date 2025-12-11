package com.dev.lib.jpa;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration
@EntityScan("com.dev.lib.jpa")
@EnableJpaRepositories(basePackages = "com.dev.lib.jpa")
public class CommonJpaAutoConfig {

}
