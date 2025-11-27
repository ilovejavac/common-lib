package com.dev.lib.security;

import com.dev.lib.exceptions.BizException;
import com.dev.lib.security.annotation.Anonymous;
import com.dev.lib.security.annotation.Internal;
import com.dev.lib.security.annotation.RequirePermission;
import com.dev.lib.security.annotation.RequireRole;
import com.dev.lib.security.util.SecurityContextHolder;
import com.dev.lib.security.util.UserDetails;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.method.HandlerMethod;

import java.util.Collection;
import java.util.Set;

public interface AuthenticateService {
    /**
     * 加载用户信息
     */
    UserDetails loadUserById(Long id);

    /**
     * 校验 token 是否有效
     */
    Boolean validToken(String token);

    Collection<UserDetails> batchLoadUserByIds(Set<Long> ids);
}
