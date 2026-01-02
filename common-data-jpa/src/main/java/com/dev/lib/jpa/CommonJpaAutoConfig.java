package com.dev.lib.jpa;

import com.dev.lib.jpa.entity.BaseRepositoryImpl;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration
@EntityScan("com.dev.lib")
@EnableJpaRepositories(
        basePackages = "com.dev.lib",
        repositoryBaseClass = BaseRepositoryImpl.class
)
public class CommonJpaAutoConfig {

}
