package com.dev.lib.security.util;

import com.dev.lib.entity.EntityStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class UserDetails implements Serializable {

    private static final String INTERNAL = "INTERNAL";

    private static final String ANONYMOUS = "ANONYMOUS";

    private static final String SYSTEM = "SYSTEM";

    private static final Long ANONYMOUS_USER_ID = -1L;

    private static final Long INTERNAL_USER_ID = -2L;

    private static final Long SYSTEM_USER_ID = -3L;

    public static final UserDetails Anonymous;

    public static final UserDetails Internal;

    public static final UserDetails System;

    static {
        Anonymous = UserDetails.builder()
                .id(ANONYMOUS_USER_ID)
                .roles(List.of(ANONYMOUS))
                .status(EntityStatus.ENABLE)
                .realName("Anonymous")
                .username("anonymous")
                .userType(ANONYMOUS)
                .tenant(ANONYMOUS_USER_ID)
                .deptId(ANONYMOUS_USER_ID)
                .deptName("Anonymous")
                .validated(false)
                .deptIds(Collections.emptySet())
                .build();

        Internal = UserDetails.builder()
                .id(INTERNAL_USER_ID)
                .roles(List.of(INTERNAL))
                .status(EntityStatus.ENABLE)
                .realName("Internal")
                .username("internal")
                .userType(INTERNAL)
                .tenant(INTERNAL_USER_ID)
                .deptId(INTERNAL_USER_ID)
                .deptName("Internal")
                .validated(true)
                .deptIds(Collections.emptySet())
                .build();

        System = UserDetails.builder()
                .id(SYSTEM_USER_ID)
                .roles(List.of(SYSTEM))
                .status(EntityStatus.ENABLE)
                .realName("System")
                .username("system")
                .userType(SYSTEM)
                .tenant(SYSTEM_USER_ID)
                .deptId(SYSTEM_USER_ID)
                .deptName("System")
                .validated(true)
                .deptIds(Collections.emptySet())
                .build();
    }

    private Boolean validated;

    // ===== 基础信息 =====
    private Long id;

    private String username;

    private Long tenant;  // 租户 ID

    private String email;

    private String phone;

    // ===== 权限信息 =====
    private List<String> permissions;

    private List<String> roles;

    // ===== 部门/组织信息 (数据权限) =====
    private Long deptId;              // 部门 ID

    private String deptName;          // 部门名称(可选,方便日志)

    private Set<Long> deptIds;        // 数据权限范围内的所有部门 ID

    // ===== 用户状态  =====
    private String realName;          // 真实姓名(用于日志/审计)

    private String userType;          // 用户类型: EMPLOYEE, ADMIN, SYSTEM 等

    private EntityStatus status;           // 用户状态

    // ===== Token  =====
    private String tokenId;           // Token 唯一标识(用于踢人/单点登录)

    private Long loginTime;           // 登录时间戳

    private Long expireTime;          // 过期时间戳

    // ===== 客户端信息 (审计/安全) =====
    private String clientIp;          // 客户端 IP

    private String clientType;        // 客户端类型: WEB, APP, MINI_PROGRAM

    private String deviceId;          // 设备 ID(可选)

    // ===== 扩展字段 =====
    private Map<String, Object> extra;  // 扩展属性(避免频繁改 UserContext)

    // ===== 工具方法 =====

    /**
     * 是否超级管理员
     */
    public boolean isSuperAdmin() {

        return roles != null && roles.contains("ADMIN");
    }

    /**
     * 是否有指定权限
     */
    public boolean hasPermission(String permission) {

        return permissions != null && permissions.contains(permission);
    }

    /**
     * 是否有指定角色
     */
    public boolean hasRole(String role) {

        return roles != null && roles.contains(role);
    }

    /**
     * 是否属于指定部门
     */
    public boolean belongsToDept(Long deptId) {

        return deptIds != null && deptIds.contains(deptId);
    }

    /**
     * Token 是否过期
     */
    public boolean isExpired() {

        return expireTime != null && java.lang.System.currentTimeMillis() > expireTime;
    }

    /**
     * 是否匿名用户
     */
    public boolean isAnonymous() {

        return ANONYMOUS_USER_ID.equals(this.id);
    }

    /**
     * 是否内部用户
     */
    public boolean isInternal() {

        return INTERNAL_USER_ID.equals(this.id);
    }

    /**
     * 是否真实用户
     */
    public boolean isRealUser() {

        return this.id != null && this.id > 0;
    }

}