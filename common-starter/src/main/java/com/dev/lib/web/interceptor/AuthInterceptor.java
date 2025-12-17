package com.dev.lib.web.interceptor;

import com.dev.lib.security.util.SecurityContextHolder;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 登录认证，权限校验
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class AuthInterceptor implements HandlerInterceptor {

    private final PermissionValidator validator;

    @Override
    public boolean preHandle(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull Object handler
    ) {

        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }

        if (validator.anonymous(handlerMethod)) {
            return true;
        }

        // 1. 放行不需要认证的接口
        if (validator.shouldSkip(request)) {
            log.info(
                    "白名单放行, {}",
                    request.getRequestURI()
            );
            return true;
        }

        // 2.解析 token 设置用户信息
        validator.setContextInfo(request);
        // 3.校验请求权限
        validator.valid(handlerMethod);

        return true;
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