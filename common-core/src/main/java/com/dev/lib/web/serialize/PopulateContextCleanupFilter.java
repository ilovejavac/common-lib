package com.dev.lib.web.serialize;

import jakarta.servlet.*;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 请求结束时清理 ThreadLocal
 */
@Component
@Order(Integer.MIN_VALUE)
public class PopulateContextCleanupFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        try {
            chain.doFilter(request, response);
        } finally {
            PopulateContextHolder.clear();
        }
    }

}