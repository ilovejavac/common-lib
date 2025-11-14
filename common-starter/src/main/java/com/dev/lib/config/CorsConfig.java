package com.dev.lib.config;

import com.dev.lib.config.properties.CorsProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.Arrays;

@Configuration
public class CorsConfig {

    @Bean
    public CorsFilter corsFilter(CorsProperties corsProperties) {
        CorsConfiguration config = new CorsConfiguration();

        if (corsProperties.getAllowedOrigins() != null) {
            corsProperties.getAllowedOrigins().forEach(config::addAllowedOrigin);
        } else {
            config.addAllowedOriginPattern("*");
        }

        if (corsProperties.getAllowedMethods() != null) {
            corsProperties.getAllowedMethods().forEach(config::addAllowedMethod);
        } else {
            config.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        }

        config.addAllowedHeader("*");
        config.setAllowCredentials(corsProperties.isAllowCredentials());
        config.setMaxAge(corsProperties.getMaxAge());

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);

        return new CorsFilter(source);
    }

    @Bean
    @ConfigurationProperties(prefix = "app.cors")
    public CorsProperties corsProperties() {
        return new CorsProperties();
    }
}