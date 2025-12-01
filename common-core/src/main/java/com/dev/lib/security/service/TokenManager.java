package com.dev.lib.security.service;

import com.dev.lib.security.model.TokenItem;
import com.dev.lib.security.model.TokenType;

import java.util.List;
import java.util.Map;

public interface TokenManager {

    Object createToken(TokenItem tokenItem);

    /**
     * 根据 token key 获取 token 对象
     *
     * @param tokenKey token key
     * @return AccessTokenPo
     */
    TokenItem getToken(String tokenKey);

    /**
     * 根据 token key 和类型获取 token 对象
     *
     * @param tokenType token 类型
     * @param tokenKey  token key
     * @return AccessTokenPo
     */
    TokenItem getToken(TokenType tokenType, String tokenKey);

    /**
     * 验证 token 是否有效
     *
     * @param tokenKey token key
     * @return 是否有效
     */
    boolean validateToken(String tokenKey);

    /**
     * 验证 token 是否有效（指定类型）
     *
     * @param tokenType token 类型
     * @param tokenKey  token key
     * @return 是否有效
     */
    boolean validateToken(TokenType tokenType, String tokenKey);

    /**
     * 刷新 token 过期时间
     *
     * @param tokenKey token key
     * @param timeout  新的过期时间（秒）
     */
    void refreshToken(String tokenKey, long timeout);
    void refreshToken(String tokenKey, String tokenValue);

    void refreshToken(String tokenKey, Map<String, Object> metadata);

    /**
     * 删除 token
     *
     * @param tokenKey token key
     */
    void deleteToken(String tokenKey);

    /**
     * 删除指定类型的 token
     *
     * @param tokenType token 类型
     * @param tokenKey  token key
     */
    void deleteToken(TokenType tokenType, String tokenKey);

    /**
     * 删除用户的所有 token
     *
     * @param userId 用户ID
     */
    void deleteUserTokens(String userId);

    /**
     * 删除用户指定类型的 token
     *
     * @param userId    用户ID
     * @param tokenType token 类型
     */
    void deleteUserTokens(String userId, TokenType tokenType);

    /**
     * 获取用户的所有 token
     *
     * @param userId 用户ID
     * @return token 列表
     */
    List<TokenItem> getUserTokens(String userId);

    /**
     * 获取用户指定类型的 token
     *
     * @param userId    用户ID
     * @param tokenType token 类型
     * @return token 列表
     */
    List<TokenItem> getUserTokens(String userId, TokenType tokenType);

    /**
     * 清理过期的 token
     *
     * @return 清理的数量
     */
    int cleanExpiredTokens();

    /**
     * 根据客户端ID获取 token（用于PUBLIC类型）
     *
     * @param clientId 客户端ID
     * @return token 列表
     */
    List<TokenItem> getClientTokens(String clientId);

    /**
     * 搜索 token
     *
     * @param prefix  前缀
     * @param keyword 关键词
     * @param start   起始位置
     * @param size    数量
     * @return token key 列表
     */
    List<String> searchTokenKeys(String prefix, String keyword, int start, int size);
}
