package com.dev.lib.security.scanner;

import com.dev.lib.security.config.properties.EndpointScannerProperties;
import com.dev.lib.security.model.EndpointPermission;
import com.dev.lib.security.service.annotation.Anonymous;
import com.dev.lib.security.service.annotation.Internal;
import com.dev.lib.security.service.annotation.RequirePermission;
import com.dev.lib.security.service.annotation.RequireRole;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.CollectionUtils;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Scanner for endpoint permissions from Controller mappings
 */
@Slf4j
@Component
@ConditionalOnWebApplication
public class EndpointPermissionScanner {

    private final RequestMappingHandlerMapping handlerMapping;
    private final EndpointScannerProperties properties;
    private final ApplicationContext applicationContext;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    public EndpointPermissionScanner(
            @Qualifier("requestMappingHandlerMapping") RequestMappingHandlerMapping handlerMapping,
            EndpointScannerProperties properties,
            ApplicationContext applicationContext) {
        this.handlerMapping = handlerMapping;
        this.properties = properties;
        this.applicationContext = applicationContext;
    }

    private Set<String> defaultBasePackages;

    /**
     * Scan all controller endpoints and extract permission information
     */
    public List<EndpointPermission> scanEndpoints(String serviceName) {
        log.info("Starting endpoint permission scanning for service: {}", serviceName);

        // Initialize default base packages from @SpringBootApplication
        if (CollectionUtils.isEmpty(properties.getBasePackages()) && defaultBasePackages == null) {
            defaultBasePackages = findSpringBootApplicationPackages();
        }

        Map<RequestMappingInfo, HandlerMethod> map = handlerMapping.getHandlerMethods();
        List<EndpointPermission> permissions = new ArrayList<>();

        for (Map.Entry<RequestMappingInfo, HandlerMethod> entry : map.entrySet()) {
            RequestMappingInfo mappingInfo = entry.getKey();
            HandlerMethod handlerMethod = entry.getValue();

            Set<String> patterns = mappingInfo.getPathPatternsCondition() != null
                    ? mappingInfo.getPathPatternsCondition().getPatterns().stream()
                        .map(p -> p.getPatternString())
                        .collect(Collectors.toSet())
                    : mappingInfo.getDirectPaths();

            Set<String> methods = mappingInfo.getMethodsCondition().getMethods().stream()
                    .map(Enum::name)
                    .collect(Collectors.toSet());

            // Check base package filter
            if (shouldScanPackage(handlerMethod.getBeanType())) {
                for (String pattern : patterns) {
                    // Skip excluded patterns
                    if (isExcludedPattern(pattern)) {
                        continue;
                    }

                    for (String method : methods) {
                        EndpointPermission permission = buildPermission(pattern, method, handlerMethod, serviceName);
                        if (permission != null) {
                            permissions.add(permission);
                        }
                    }
                }
            }
        }

        log.info("Scanned {} endpoint permissions for service: {}", permissions.size(), serviceName);
        return permissions;
    }

    /**
     * Find all packages annotated with @SpringBootApplication
     */
    private Set<String> findSpringBootApplicationPackages() {
        Set<String> packages = new HashSet<>();
        String[] beanNames = applicationContext.getBeanNamesForAnnotation(SpringBootApplication.class);

        for (String beanName : beanNames) {
            Class<?> beanClass = applicationContext.getType(beanName);
            if (beanClass != null && beanClass.getPackage() != null) {
                String packageName = beanClass.getPackage().getName();
                packages.add(packageName);
                log.debug("Found @SpringBootApplication package: {}", packageName);
            }
        }

        if (packages.isEmpty()) {
            log.warn("No @SpringBootApplication found, scanning all packages");
        } else {
            log.info("Default scan packages: {}", packages);
        }

        return packages;
    }

    /**
     * Check if the controller class should be scanned based on base packages
     */
    private boolean shouldScanPackage(Class<?> beanType) {
        Set<String> packagesToScan = CollectionUtils.isEmpty(properties.getBasePackages())
                ? defaultBasePackages
                : properties.getBasePackages();

        // If no packages configured and no @SpringBootApplication found, scan all
        if (CollectionUtils.isEmpty(packagesToScan)) {
            return true;
        }

        String packageName = beanType.getPackage().getName();
        return packagesToScan.stream()
                .anyMatch(basePackage -> packageName.startsWith(basePackage));
    }

    /**
     * Check if the pattern matches any excluded pattern
     */
    private boolean isExcludedPattern(String pattern) {
        if (CollectionUtils.isEmpty(properties.getExcludePatterns())) {
            return false;
        }

        return properties.getExcludePatterns().stream()
                .anyMatch(exclude -> pathMatcher.match(exclude, pattern));
    }

    /**
     * Build EndpointPermission from handler method
     * Returns null if no security annotation is present
     */
    private EndpointPermission buildPermission(String path, String httpMethod,
                                               HandlerMethod handlerMethod, String serviceName) {
        Class<?> beanType = handlerMethod.getBeanType();
        java.lang.reflect.Method method = handlerMethod.getMethod();

        // METHOD level annotations take precedence over TYPE level
        Anonymous anonymous = AnnotatedElementUtils.findMergedAnnotation(method, Anonymous.class);
        if (anonymous == null) {
            anonymous = AnnotatedElementUtils.findMergedAnnotation(beanType, Anonymous.class);
        }

        Internal internal = AnnotatedElementUtils.findMergedAnnotation(method, Internal.class);
        if (internal == null) {
            internal = AnnotatedElementUtils.findMergedAnnotation(beanType, Internal.class);
        }

        RequirePermission requirePermission = AnnotatedElementUtils.findMergedAnnotation(method, RequirePermission.class);
        if (requirePermission == null) {
            requirePermission = AnnotatedElementUtils.findMergedAnnotation(beanType, RequirePermission.class);
        }

        RequireRole requireRole = AnnotatedElementUtils.findMergedAnnotation(method, RequireRole.class);
        if (requireRole == null) {
            requireRole = AnnotatedElementUtils.findMergedAnnotation(beanType, RequireRole.class);
        }

        // Skip if no security annotation is present
        if (anonymous == null && internal == null && requirePermission == null && requireRole == null) {
            return null;
        }

        // Build permission object
        EndpointPermission permission = new EndpointPermission();
        permission.setService(serviceName);
        permission.setPath(path);
        permission.setMethod(httpMethod);
        permission.setAnonymous(anonymous != null);
        permission.setInternal(internal != null);
        permission.setPermissions(requirePermission != null ? requirePermission.value() : new String[0]);
        permission.setRoles(requireRole != null ? requireRole.value() : new String[0]);

        return permission;
    }

}
