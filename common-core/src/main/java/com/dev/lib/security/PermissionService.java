package com.dev.lib.security;

public interface PermissionService {
    /**
     * 当前用户是否拥有权限
     */
    boolean hasPermission(String... permissions);

    /**
     * 当前用户是否拥有角色
     */
    boolean hasRole(String... roles);
}
