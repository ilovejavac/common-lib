package com.dev.lib.jpa;

import com.dev.lib.jpa.config.BaseRepositoryFactoryBeanPostProcessor;
import com.dev.lib.jpa.config.CommonJpaPackageRegistrar;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.data.jpa.autoconfigure.DataJpaRepositoriesAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

@AutoConfiguration(before = DataJpaRepositoriesAutoConfiguration.class)
@Import(CommonJpaPackageRegistrar.class)
public class CommonJpaAutoConfig {

    @Bean
    public static BaseRepositoryFactoryBeanPostProcessor baseRepositoryFactoryBeanPostProcessor() {

        return new BaseRepositoryFactoryBeanPostProcessor();
    }

}
