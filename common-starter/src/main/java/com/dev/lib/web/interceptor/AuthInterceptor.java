package com.dev.lib.web.interceptor;

import com.dev.lib.config.properties.AppSecurityProperties;
import com.dev.lib.exceptions.BizException;
import com.dev.lib.security.AuthenticateService;
import com.dev.lib.security.PermissionService;
import com.dev.lib.security.annotation.Anonymous;
import com.dev.lib.security.annotation.Internal;
import com.dev.lib.security.annotation.RequirePermission;
import com.dev.lib.security.annotation.RequireRole;
import com.dev.lib.security.util.SecurityContextHolder;
import com.dev.lib.security.util.UserDetails;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.PathMatcher;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * 登录认证，权限校验
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class AuthInterceptor implements HandlerInterceptor, InitializingBean {

    private final AuthenticateService authenticateService;
    private final AppSecurityProperties securityProperties;
    private final PermissionService permissionService;
    // 以下接口放行
    private final Set<String> whitelistPatterns = new HashSet<>(
            Arrays.asList(
                    "/healthz", "/", "/actuator/**", "/error", "/favicon.ico",
                    "/api/login", "/api/register", "/api/public/**", "/swagger-ui/**", "/v3/api-docs/**"
            )
    );
    private final PathMatcher pathMatcher = new AntPathMatcher();

    private boolean isWhitelistRequest(String uri) {
        return whitelistPatterns.stream()
                .anyMatch(pattern -> pathMatcher.match(pattern, uri));
    }

    @Override
    public void afterPropertiesSet() {
        if (securityProperties.getWhiteListRequest() != null) {
            whitelistPatterns.addAll(securityProperties.getWhiteListRequest());
        }
    }

    @Override
    public boolean preHandle(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull Object handler
    ) {
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }

        // 1. 放行不需要认证的接口 =====
        if (isWhitelistRequest(request.getRequestURI())) {
            log.info("白名单放行, {}", request.getRequestURI());
            return true;
        }

        Class<?> controllerClass = handlerMethod.getBeanType();

        // 2. 方法级别 @Anonymous 优先检查
        if (handlerMethod.hasMethodAnnotation(Anonymous.class)) {
            anonymous();
            return true;
        }

        // 3. 类级别 @Anonymous
        if (controllerClass.isAnnotationPresent(Anonymous.class)) {
            anonymous();
            return true;
        }

        // 4. 方法级别 @Internal 优先检查
        Internal methodInternal = handlerMethod.getMethodAnnotation(Internal.class);
        if (methodInternal != null) {
            internal(request);
            return true;
        }

        // 5. 类级别 @Internal
        Internal classInternal = controllerClass.getAnnotation(Internal.class);
        if (classInternal != null) {
            internal(request);
            return true;
        }

        // 6. 必须登录
        if (!SecurityContextHolder.isLogin()) {
            throw new BizException(401, "认证失败，请先登录");
        }

        // 7. 方法级别 @RequireRole
        RequireRole methodRole = handlerMethod.getMethodAnnotation(RequireRole.class);
        if (methodRole != null) {
            if (!permissionService.hasRole(methodRole.value())) {
                throw new BizException(403, "无权限访问");
            }
            return true;
        }

        // 8. 类级别 @RequireRole
        RequireRole classRole = controllerClass.getAnnotation(RequireRole.class);
        if (classRole != null && !permissionService.hasRole(classRole.value())) {
            throw new BizException(403, "无权限访问");
        }


        // 9. 方法级别 @RequirePermission
        RequirePermission methodPermission = handlerMethod.getMethodAnnotation(RequirePermission.class);
        if (methodPermission != null) {
            if (!permissionService.hasPermission(methodPermission.value())) {
                throw new BizException(403, "无权限访问");
            }
            return true;
        }

        // 10. 类级别 @RequirePermission
        RequirePermission classPermission = controllerClass.getAnnotation(RequirePermission.class);
        if (classPermission != null && !permissionService.hasPermission(classPermission.value())) {
            throw new BizException(403, "无权限访问");
        }


        return true;
    }

    private static void anonymous() {
        if (!SecurityContextHolder.isLogin()) {
            SecurityContextHolder.set(UserDetails.Anonymous);
        }
    }

    private void internal(HttpServletRequest request) {
        String token = request.getHeader("X-Internal-Id");
        if (Boolean.FALSE.equals(authenticateService.validToken(token))) {
            throw new BizException(403, "服务认证失败");
        }
        if (!SecurityContextHolder.isLogin()) {
            SecurityContextHolder.set(UserDetails.Internal.setTokenId(token));
        }
    }

    @Override
    public void afterCompletion(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull Object handler,
            Exception ex
    ) {
        // 清理 ThreadLocal
        SecurityContextHolder.clear();
    }
}