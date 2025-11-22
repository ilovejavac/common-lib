package com.dev.lib.config;

import com.dev.lib.config.properties.AppCloudProperties;
import com.dev.lib.config.properties.AppDubboProperties;
import com.dev.lib.config.properties.AppSecurityProperties;
import com.dev.lib.config.properties.AppSnowFlakeProperties;
import com.dev.lib.config.properties.AppSqlMonitorProperties;
import com.dev.lib.config.properties.AppStorageProperties;
import com.dev.lib.security.InternalService;
import com.dev.lib.security.UserDetailService;
import io.github.linpeilie.annotations.ComponentModelConfig;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@AutoConfiguration
@ComponentScan("com.dev.lib")
@EntityScan("com.dev.lib")  // 添加
@EnableJpaRepositories(basePackages = "com.dev.lib")  // 添加
@ComponentModelConfig
public class CommonAutoConfiguration {

    @Bean
    @ConfigurationProperties(prefix = "app.cloud")
    public AppCloudProperties appCloudProperties() {
        return new AppCloudProperties();
    }

    @Bean
    @ConfigurationProperties(prefix = "app.dubbo")
    public AppDubboProperties appDubboProperties() {
        return new AppDubboProperties();
    }

    @Bean
    @ConfigurationProperties(prefix = "app.security")
    public AppSecurityProperties appSecurityProperties() {
        return new AppSecurityProperties();
    }

    @Bean
    @ConfigurationProperties(prefix = "app.storage")
    public AppStorageProperties appStorageProperties() {
        return new AppStorageProperties();
    }

    @Bean
    @ConfigurationProperties(prefix = "app.sql")
    public AppSqlMonitorProperties appSqlMonitorProperties() {
        return new AppSqlMonitorProperties();
    }

    @Bean
    @ConfigurationProperties(prefix = "app.snow-flake")
    public AppSnowFlakeProperties appSnowFlakeProperties() {
        return new AppSnowFlakeProperties();
    }


    @Configuration
    @ConditionalOnMissingBean(UserDetailService.class)
    public static class UserDetailAutoConfig {
        @DubboReference
        private UserDetailService userDetailService;
    }

    @Configuration
    @ConditionalOnMissingBean(InternalService.class)
    public static class InternalAutoConfig {
        @DubboReference
        private InternalService internalService;
    }
}