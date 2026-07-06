package com.dev.lib.security.service;

import com.dev.lib.security.util.UserDetails;
import com.dev.lib.util.ImmutablePair;

public interface TokenService {

    /**
     * 生成 access token 和 refresh token
     */
    ImmutablePair<String, String> generateToken(UserDetails userDetails);

    /**
     * 使用 refresh token 生成新的 access token 和 refresh token
     */
    default ImmutablePair<String, String> refreshAccessToken(String refreshToken) {

        throw new UnsupportedOperationException("refresh token is not supported");
    }

    /**
     * 解析 token
     */
    UserDetails parseToken(String token);

}
