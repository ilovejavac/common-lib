package com.dev.lib.security.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Set;

/**
 * Endpoint scanner configuration properties
 */
@Data
@ConfigurationProperties(prefix = "app.security.endpoint-scanner")
public class EndpointScannerProperties {

    /**
     * Enable endpoint permission scanning
     */
    private boolean enabled = true;

    /**
     * Service name for permission registration
     * Default uses spring.application.name
     */
    private String serviceName;

    /**
     * Exclude patterns from scanning
     */
    private Set<String> excludePatterns = Set.of("/healthz", "/actuator/**", "/error");

    /**
     * Limit scan to specific base packages (optional)
     */
    private Set<String> basePackages;

    /**
     * Log registration details
     */
    private boolean logRegistration = true;

}
