package com.dev.lib.security;

import com.dev.lib.security.util.UserDetails;

public interface UserDetailService {
    /**
     * 加载用户信息
     */
    UserDetails loadUserById(Long id);
}
