package com.dev.lib.security;

import com.dev.lib.security.util.UserDetails;

public interface AuthenticateService {
    /**
     * 加载用户信息
     */
    UserDetails loadUserById(Long id);

    /**
     * 校验 token 是否有效
     */
    Boolean validToken(String token);
}
