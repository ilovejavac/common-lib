package com.dev.lib.security.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

@Data
@ConfigurationProperties(prefix = "app.security.valid")
public class SecurityValidProperties {
    private List<String> skipPatterns = new ArrayList<>();
}
