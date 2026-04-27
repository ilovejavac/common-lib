package com.dev.lib.security.jwt;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "app.security.jwt")
public class JwtSecurityProperties {

    private String secret;

    private Long expiration;
}
