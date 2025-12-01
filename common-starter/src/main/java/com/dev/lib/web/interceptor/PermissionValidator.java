package com.dev.lib.web.interceptor;

import cn.dev33.satoken.stp.StpUtil;
import com.dev.lib.config.properties.AppSecurityProperties;
import com.dev.lib.exceptions.BizException;
import com.dev.lib.security.service.AuthenticateService;
import com.dev.lib.security.service.PermissionService;
import com.dev.lib.security.service.TokenService;
import com.dev.lib.security.service.annotation.Anonymous;
import com.dev.lib.security.service.annotation.RequirePermission;
import com.dev.lib.security.service.annotation.RequireRole;
import com.dev.lib.security.util.ClientInfoExtractor;
import com.dev.lib.security.util.SecurityContextHolder;
import com.dev.lib.security.util.UserDetails;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.PathMatcher;
import org.springframework.web.method.HandlerMethod;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class PermissionValidator implements InitializingBean {
    private final PermissionService permissionService;
    private final AuthenticateService authenticateService;
    private final TokenService tokenService;

    public void valid(
            HttpServletRequest request,
            HandlerMethod handlerMethod
    ) {
        Class<?> controllerClass = handlerMethod.getBeanType();

        // 2. 方法级别 @Anonymous 优先检查
        if (handlerMethod.hasMethodAnnotation(Anonymous.class)) {
            anonymous();
            return;
        }

        // 3. 类级别 @Anonymous
        if (controllerClass.isAnnotationPresent(Anonymous.class)) {
            anonymous();
            return;
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
            return;
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
            return;
        }

        // 10. 类级别 @RequirePermission
        RequirePermission classPermission = controllerClass.getAnnotation(RequirePermission.class);
        if (classPermission != null && !permissionService.hasPermission(classPermission.value())) {
            throw new BizException(403, "无权限访问");
        }
    }

    private void anonymous() {
        if (!SecurityContextHolder.isLogin()) {
            SecurityContextHolder.set(UserDetails.Anonymous);
        }
    }

//    // 4. 方法级别 @Internal 优先检查
//    Internal methodInternal = handlerMethod.getMethodAnnotation(Internal.class);
//        if (methodInternal != null) {
//        internal(request);
//        return;
//    }
//
//    // 5. 类级别 @Internal
//    Internal classInternal = controllerClass.getAnnotation(Internal.class);
//        if (classInternal != null) {
//        internal(request);
//        return;
//    }
//    private void internal(HttpServletRequest request) {
//        String token = request.getHeader("X-Internal-Id");
//        if (Boolean.FALSE.equals(authenticateService.validToken(token))) {
//            throw new BizException(403, "服务认证失败");
//        }
//        if (!SecurityContextHolder.isLogin()) {
//            SecurityContextHolder.set(UserDetails.Internal.setTokenId(token));
//        }
//    }

    public boolean shouldSkip(HttpServletRequest request) {
        if (SecurityContextHolder.validated()) {
            return true;
        }
        return isWhitelistRequest(request.getRequestURI());
    }

    private final AppSecurityProperties securityProperties;

    // 以下接口放行
    private final Set<String> whitelistPatterns = new HashSet<>(
            Arrays.asList(
                    "/",
                    "/error",
                    "/healthz",
                    "/api/login",
                    "/actuator/**",
                    "/favicon.ico",
                    "/api/register",
                    "/api/public/**",
                    "/swagger-ui/**",
                    "/v3/api-docs/**"
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

    private String extractToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        return (bearerToken != null && bearerToken.startsWith("Bearer "))
                ? bearerToken.substring(7)
                : null;
    }

    public void setContextInfo(HttpServletRequest request) {
        try {
            UserDetails userDetail = tokenService.parseToken(StpUtil.getTokenValue());
            if (userDetail != null) {
                userDetail.setClientIp(ClientInfoExtractor.getClientIp(request));
                userDetail.setClientType(ClientInfoExtractor.getClientType(request));
                userDetail.setDeviceId(request.getHeader("X-Device-Id"));

                SecurityContextHolder.set(userDetail);
            }
        } finally {
            log.warn("access with anonymous");
        }
//        String token = extractToken(request);
//        UserDetails userDetail = tokenService.parseToken(token);
//
//        if (userDetail != null && !userDetail.isExpired()) {
//            userDetail.setClientIp(ClientInfoExtractor.getClientIp(request));
//            userDetail.setClientType(ClientInfoExtractor.getClientType(request));
//            userDetail.setDeviceId(request.getHeader("X-Device-Id"));
//
//            SecurityContextHolder.set(userDetail);
//        }
    }
}
