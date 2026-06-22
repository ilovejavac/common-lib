package com.dev.lib.security.config;

import com.dev.lib.security.config.properties.EndpointScannerProperties;
import com.dev.lib.security.config.properties.SecurityValidProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@ComponentScan
@Configuration
@RequiredArgsConstructor
@EnableConfigurationProperties({EndpointScannerProperties.class, SecurityValidProperties.class})
public class SecurityConfig {

}