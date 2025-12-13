package com.dev.lib.jpa;

import com.dev.lib.jpa.entity.BaseRepositoryImpl;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration
@EntityScan("com.dev.lib.jpa")
@EnableJpaRepositories(
        repositoryBaseClass = BaseRepositoryImpl.class
)
//@EnableBaseJpaRepositories("com.dev.lib.jpa")
public class CommonJpaAutoConfig {

}
