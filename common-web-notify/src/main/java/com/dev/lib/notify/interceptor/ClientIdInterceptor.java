package com.dev.lib.notify.interceptor;

import com.dev.lib.notify.context.ClientIdHolder;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Client ID 拦截器
 * 从请求头中提取 client-id 并存储到 ThreadLocal
 */
@Slf4j
@Component
public class ClientIdInterceptor implements HandlerInterceptor {

    /**
     * 请求头名称：Client ID
     */
    public static final String CLIENT_ID_HEADER = "G-Client-Id";

    /**
     * 请求参数名称：Client ID
     */
    public static final String CLIENT_ID_PARAM = "clientId";

    @Override
    public boolean preHandle(
            HttpServletRequest request,
            @Nullable HttpServletResponse response,
            @Nullable Object handler
    ) {
        // 首先尝试从请求头获取
        String clientId = request.getHeader(CLIENT_ID_HEADER);

        // 如果请求头没有，尝试从参数获取
        if (clientId == null || clientId.isEmpty()) {
            clientId = request.getParameter(CLIENT_ID_PARAM);
        }

        // 存储到 ThreadLocal
        if (clientId != null && !clientId.isEmpty()) {
            ClientIdHolder.setClientId(clientId);
            log.debug("Client ID extracted from request: {}", clientId);
        }

        return true;
    }

    @Override
    public void afterCompletion(
            HttpServletRequest request,
            @Nullable HttpServletResponse response,
            @Nullable Object handler,
            @Nullable Exception ex
    ) {
        // 清理 ThreadLocal
        ClientIdHolder.clear();
    }
}
