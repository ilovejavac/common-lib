package com.dev.lib.security;

public interface InternalService {
    /**
     * 校验 token 是否有效
     */
    boolean validToken(String token);
}
