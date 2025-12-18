package com.dev.lib.security.service;

import com.dev.lib.security.util.UserDetails;

public interface TokenService {

    /**
     * 生成 token
     */
    String generateToken(UserDetails userDetails);

    /**
     * 解析 token
     */
    UserDetails parseToken(String token);

}
