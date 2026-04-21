package com.dev.lib.security.config;

import com.dev.lib.security.config.properties.EndpointScannerProperties;
import com.dev.lib.security.model.EndpointPermission;
import com.dev.lib.security.scanner.EndpointPermissionScanner;
import com.dev.lib.security.service.AuthenticateService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.core.env.Environment;

import java.util.List;

/**
 * Configuration for automatic endpoint permission registration
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
@ConditionalOnWebApplication
@ConditionalOnBean(AuthenticateService.class)
public class PermissionRegistrationConfig {

    private final AuthenticateService authenticateService;
    private final EndpointPermissionScanner scanner;
    private final EndpointScannerProperties properties;
    private final Environment environment;

    @PostConstruct
    public void registerPermissions() {
        if (!properties.isEnabled()) {
            log.info("Endpoint permission scanner is disabled");
            return;
        }

        String serviceName = getServiceName();

        try {
            List<EndpointPermission> permissions = scanner.scanEndpoints(serviceName);

            if (properties.isLogRegistration()) {
                logRegistrationDetails(permissions, serviceName);
            }

            authenticateService.registerPermissions(permissions);
            log.info("Successfully registered {} endpoint permissions", permissions.size());

        } catch (Exception e) {
            log.error("Failed to register endpoint permissions", e);
            throw e;
        }
    }

    /**
     * Get service name from properties or spring.application.name
     */
    private String getServiceName() {
        if (properties.getServiceName() != null && !properties.getServiceName().isBlank()) {
            return properties.getServiceName();
        }
        return environment.getProperty("spring.application.name", "unknown-service");
    }

    /**
     * Log detailed registration information
     */
    private void logRegistrationDetails(List<EndpointPermission> permissions, String serviceName) {
        log.info("Registered Endpoint Permissions:");
        log.info("================================");
        for (EndpointPermission p : permissions) {
            log.info("  {} {} | permissions={}, roles={}, anonymous={}, internal={}",
                    p.getMethod(),
                    p.getPath(),
                    String.join(",", p.getPermissions()),
                    String.join(",", p.getRoles()),
                    p.isAnonymous(),
                    p.isInternal());
        }
        log.info("================================");
    }

}
