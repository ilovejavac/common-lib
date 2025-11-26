package com.dev.lib.config.properties;

import lombok.Data;

import java.util.List;

@Data
public class CorsProperties {
    private List<String> allowedOrigins;
    private List<String> allowedMethods;
    private Long maxAge = 3600L;
    private boolean allowCredentials = true;
}