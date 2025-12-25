package com.dev.lib.security.interceptor;

import com.dev.lib.security.service.annotation.Internal;
import com.dev.lib.security.util.SecurityContextHolder;
import com.dev.lib.security.util.UserDetails;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

@Slf4j
@Order(2)
@Component
@RequiredArgsConstructor
public class InternalInterceptor implements HandlerInterceptor {

    private final PermissionValidator validator;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {

        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }

        if (validator.anonymous(handlerMethod)) {
            return true;
        }

        Class<?> controllerClass = handlerMethod.getBeanType();

        Internal methodInternal = handlerMethod.getMethodAnnotation(Internal.class);
        if (methodInternal != null) {
            return internal(request);
        }

        Internal classInternal = controllerClass.getAnnotation(Internal.class);
        if (classInternal != null) {
            return internal(request);
        }

        return true;
    }

    private boolean internal(HttpServletRequest request) {

        String token = request.getHeader("X-Internal-Id");
//        if (Boolean.FALSE.equals(authenticateService.validToken(token))) {
//            throw new BizException(403, "服务认证失败");
//        }
        if (!SecurityContextHolder.isLogin()) {
            SecurityContextHolder.set(UserDetails.Internal.setTokenId(token));
        }

        return true;
    }

}
