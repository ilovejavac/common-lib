package com.dev.lib.security.util;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.jwt.JWT;
import cn.hutool.jwt.JWTValidator;
import com.dev.lib.config.properties.AppSecurityProperties;
import com.dev.lib.security.TokenService;
import com.dev.lib.security.UserDetailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
class JwtUtil implements TokenService, InitializingBean {

    private final AppSecurityProperties securityProperties;
    private final UserDetailService userDetailService;
    private byte[] secretKeyBytes;

    @Override
    public void afterPropertiesSet() throws Exception {
        secretKeyBytes = securityProperties.getSecret().getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public String generateToken(UserDetails userDetails) {
        Date now = new Date();
        Date expiryDate = new Date(Optional.ofNullable(userDetails.getExpireTime())
                .orElse(now.getTime()) + securityProperties.getExpiration());
        log.info("Generating token for user: {}", userDetails.getUsername());

        return JWT.create()
                .setPayload("userId", userDetails.getId())
                .setPayload("username", userDetails.getUsername())
                .setPayload("iat", now.getTime() / 1000)
                .setPayload("exp", expiryDate.getTime() / 1000)
                .setKey(secretKeyBytes)
                .sign();
    }

    @Override
    public UserDetails parseToken(String token) {
        if (!StringUtils.hasText(token) || !validateToken(token)) {
            return null;
        }

        try {
            Long userId = getUserIdFromToken(token);
            return userDetailService.loadUserById(userId);
        } catch (Exception e) {
            log.error("Failed to parse user from token: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 验证token
     */
    public boolean validateToken(String token) {
        if (!StringUtils.hasText(token)) {
            return false;
        }

        try {
            JWT jwt = JWT.of(token);

            // 验证签名
            if (!jwt.setKey(secretKeyBytes).verify()) {
                log.error("Invalid JWT signature");
                return false;
            }

            // 验证过期时间
            JWTValidator.of(jwt).validateDate(DateUtil.date());

            return true;
        } catch (Exception e) {
            log.error("JWT validation failed: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * 从token获取JWT对象
     */
    private JWT parseJwt(String token) {
        if (!StringUtils.hasText(token)) {
            throw new IllegalArgumentException("Token cannot be null or empty");
        }
        return JWT.of(token).setKey(secretKeyBytes);
    }

    /**
     * 从payload中安全获取Long类型值
     */
    private Long getLongPayload(JWT jwt, String key) {
        Object value = jwt.getPayload(key);
        if (value == null) {
            throw new IllegalArgumentException("Payload key '" + key + "' not found");
        }

        return convertToLong(value, key);
    }

    /**
     * 从payload中安全获取String类型值
     */
    private String getStringPayload(JWT jwt, String key) {
        Object value = jwt.getPayload(key);
        if (value == null) {
            throw new IllegalArgumentException("Payload key '" + key + "' not found");
        }

        if (value instanceof String) {
            return (String) value;
        }
        return String.valueOf(value);
    }

    /**
     * 从payload中安全获取集合类型值
     */
    @SuppressWarnings("unchecked")
    private <T> Collection<T> getCollectionPayload(JWT jwt, String key, Class<T> elementType) {
        Object value = jwt.getPayload(key);
        if (value == null) {
            return Collections.emptyList();
        }

        if (value instanceof Collection) {
            return (Collection<T>) value;
        }

        log.warn("Payload key '{}' is not a collection type: {}", key, value.getClass().getSimpleName());
        return Collections.emptyList();
    }

    /**
     * 统一处理数字类型转换
     */
    private Long convertToLong(Object value, String key) {
        try {
            if (value instanceof Number) {
                return ((Number) value).longValue();
            } else if (value instanceof String) {
                return NumberUtil.parseLong((String) value);
            } else {
                String stringValue = String.valueOf(value);
                return NumberUtil.parseLong(stringValue);
            }
        } catch (Exception e) {
            log.error("Failed to convert payload '{}' value '{}' to Long", key, value);
            throw new IllegalArgumentException("Invalid " + key + " type in token", e);
        }
    }

    /**
     * 从token获取用户ID
     */
    public Long getUserIdFromToken(String token) {
        JWT jwt = parseJwt(token);
        return getLongPayload(jwt, "userId");
    }

    /**
     * 从token获取用户名
     */
    public String getUsernameFromToken(String token) {
        JWT jwt = parseJwt(token);
        return getStringPayload(jwt, "username");
    }

    /**
     * 从token获取权限集合
     */
    public Set<String> getPermissionsFromToken(String token) {
        JWT jwt = parseJwt(token);
        Collection<String> permissions = getCollectionPayload(jwt, "permissions", String.class);
        return new HashSet<>(permissions);
    }

    /**
     * 从token获取角色列表
     */
    public Set<String> getRolesFromToken(String token) {
        JWT jwt = parseJwt(token);
        Collection<String> roles = getCollectionPayload(jwt, "roles", String.class);
        return new HashSet<>(roles);
    }

    /**
     * 从token获取过期时间
     */
    public Date getExpirationFromToken(String token) {
        JWT jwt = parseJwt(token);
        Long expSeconds = getLongPayload(jwt, "exp");
        return new Date(expSeconds * 1000);
    }

    /**
     * 判断token是否过期
     */
    public boolean isTokenExpired(String token) {
        if (!StringUtils.hasText(token)) {
            return true;
        }

        try {
            Date expiration = getExpirationFromToken(token);
            return expiration.before(new Date());
        } catch (Exception e) {
            log.error("Failed to check token expiration: {}", e.getMessage());
            return true;
        }
    }

    /**
     * 刷新token
     */
    public String refreshToken(String token) {
        if (!validateToken(token)) {
            throw new IllegalArgumentException("Invalid token for refresh");
        }

        UserDetails userDetails = parseToken(token);
        if (userDetails == null) {
            throw new IllegalArgumentException("User not found for token");
        }

        return generateToken(userDetails);
    }

    /**
     * 获取token剩余有效时间（毫秒）
     */
    public long getTokenRemainingTime(String token) {
        if (!validateToken(token)) {
            return 0;
        }

        try {
            Date expiration = getExpirationFromToken(token);
            long remaining = expiration.getTime() - System.currentTimeMillis();
            return Math.max(0, remaining); // 确保不返回负数
        } catch (Exception e) {
            log.error("Failed to get token remaining time: {}", e.getMessage());
            return 0;
        }
    }

    /**
     * 获取token的Issue时间
     */
    public Date getIssuedAtFromToken(String token) {
        JWT jwt = parseJwt(token);
        Long iatSeconds = getLongPayload(jwt, "iat");
        return new Date(iatSeconds * 1000);
    }

    /**
     * 检查token是否在有效期内且未过期
     */
    public boolean isTokenValid(String token) {
        return validateToken(token) && !isTokenExpired(token);
    }
}