package com.dev.lib.security.domain;

import cn.dev33.satoken.dao.auto.SaTokenDaoByStringFollowObject;
import com.dev.lib.entity.EntityStatus;
import com.dev.lib.security.model.TokenItem;
import com.dev.lib.security.model.TokenType;
import com.dev.lib.security.service.TokenManager;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Maps;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class DbSaTokenDao implements SaTokenDaoByStringFollowObject {
    private final TokenManager tokenManager;
    private final ObjectMapper objectMapper;

    // ------------------------ Object 读写操作
    @Override
    public Object getObject(String key) {
        TokenItem token = tokenManager.getToken(key);
        if (token == null) {
            return null;
        }

        // ✅ 优先从 metadata 读取复杂对象
        if (token.getMetadata() != null && !token.getMetadata().isEmpty()) {
            return deserializeFromMetadata(token.getMetadata());
        }

        // ✅ 否则从 token_value 读取简单字符串
        return token.getTokenValue();
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getObject(String key, Class<T> classType) {
        return (T) getObject(key);
    }

    @Override
    public void setObject(String key, Object object, long timeout) {
        try {
            TokenItem tokenItem = new TokenItem()
                    .setTokenKey(key)
                    .setExpireTime(timeout)
                    .setStatus(EntityStatus.ENABLE)
                    .setTokenType(TokenType.ACCESS);

            // ✅ 判断是简单值还是复杂对象
            if (isSimpleValue(object)) {
                // 简单值存到 token_value
                tokenItem.setTokenValue(object.toString());
                tokenItem.setMetadata(null);
            } else {
                // 复杂对象存到 metadata
                tokenItem.setTokenValue(null);
                tokenItem.setMetadata(serializeToMetadata(object));
            }

            tokenManager.createToken(tokenItem);

        } catch (Exception e) {
            log.error("Failed to set object for key: {}", key, e);
            throw new RuntimeException("Failed to save token", e);
        }
    }

    @Override
    public void updateObject(String key, Object object) {
        try {
            if (isSimpleValue(object)) {
                tokenManager.refreshToken(key, object.toString());
            } else {
                tokenManager.refreshToken(key, serializeToMetadata(object));
            }
        } catch (Exception e) {
            log.error("Failed to update object for key: {}", key, e);
        }
    }

    @Override
    public void deleteObject(String key) {
        tokenManager.deleteToken(key);
    }

    @Override
    public long getObjectTimeout(String key) {
        return Optional.ofNullable(tokenManager.getToken(key)).map(TokenItem::getExpireTime).orElse(0L);
    }

    @Override
    public void updateObjectTimeout(String key, long timeout) {
        tokenManager.refreshToken(key, timeout);
    }


    // --------- 会话管理

    @Override
    public List<String> searchData(String prefix, String keyword, int start, int size, boolean sortType) {
        return tokenManager.searchTokenKeys(prefix, keyword, start, size);
    }

    /**
     * 判断是否为简单值
     */
    private boolean isSimpleValue(Object object) {
        if (object == null) {
            return true;
        }

        return object instanceof String
                || object instanceof Number
                || object instanceof Boolean
                || object instanceof Character;
    }

    /**
     * 序列化复杂对象到 Map（用于 metadata）
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> serializeToMetadata(Object object) {
        try {
            if (object == null) {
                return null;
            }

            // 如果已经是 Map，直接返回
            if (object instanceof Map) {
                return (Map<String, Object>) object;
            }

            // 序列化为 JSON，再转为 Map
            String json = objectMapper.writeValueAsString(object);
            return objectMapper.readValue(
                    json, new TypeReference<Map<String, Object>>() {
                    }
            );

        } catch (Exception e) {
            log.error("Failed to serialize object to metadata: {}", object.getClass(), e);
            return Maps.newHashMap();
        }
    }

    /**
     * 从 metadata 反序列化对象
     */
    private Object deserializeFromMetadata(Map<String, Object> metadata) {
        try {
            if (metadata == null || metadata.isEmpty()) {
                return null;
            }

            // 判断是否为 SaSession
            if (metadata.containsKey("loginId") || metadata.containsKey("type")) {
                String json = objectMapper.writeValueAsString(metadata);
                return objectMapper.readValue(json, cn.dev33.satoken.session.SaSession.class);
            }

            // 其他情况返回 Map
            return metadata;

        } catch (Exception e) {
            log.warn("Failed to deserialize metadata, returning raw map", e);
            return metadata;
        }
    }
}
