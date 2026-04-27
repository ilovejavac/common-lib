package com.dev.lib.security.jwt;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(JwtSecurityProperties.class)
public class JwtSecurityConfig {
}
