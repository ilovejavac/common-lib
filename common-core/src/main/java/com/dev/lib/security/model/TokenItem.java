package com.dev.lib.security.model;

import com.dev.lib.entity.EntityStatus;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;
import java.util.Map;

@Data
public class TokenItem implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private List<String> permissions;

    private List<String> roles;

    private String userId;

    private String tokenKey;

    private String tokenValue;

    private EntityStatus status;

    private TokenType tokenType;

    private Long expireTime;

    private String clientId;
    private Map<String, Object> metadata;
}
