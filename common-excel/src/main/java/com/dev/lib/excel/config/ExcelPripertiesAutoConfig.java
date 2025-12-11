package com.dev.lib.excel.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ExcelPripertiesAutoConfig {

    @Bean
    @ConfigurationProperties(prefix = "app.excel")
    public AppExcelProperties appExcelProperties() {

        return new AppExcelProperties();
    }

}
