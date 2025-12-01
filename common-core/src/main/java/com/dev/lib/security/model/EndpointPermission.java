package com.dev.lib.security.model;

import lombok.Data;

@Data
public class EndpointPermission {
    private String service;
    private String path;           // 接口路径
    private String method;         // HTTP 方法
    private String[] permissions;  // 需要的权限 (来自 @RequirePermission)
    private String[] roles;        // 需要的角色 (来自 @RequireRole)
    private boolean anonymous;     // 是否匿名 (来自 @Anonymous)
    private boolean internal;      // 是否内部接口 (来自 @Internal)
}
