package com.dev.lib.security.util;

import com.dev.lib.entity.EntityStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class UserDetails implements Serializable {

    public static final UserDetails Anonymous;
    private static final String INTERNAL = "INTERNAL";
    private static final String ANONYMOUS = "ANONYMOUS";

    // 外部用户
    public static UserDetails internal() {
        return UserDetails.builder().id(20L)
                .roles(Set.of(INTERNAL))
                .status(EntityStatus.ENABLE)
                .realName(INTERNAL.toLowerCase())
                .username(INTERNAL.toLowerCase())
                .userType(INTERNAL)
                .tenant(0L)
                .deptId(0L)
                .deptName(INTERNAL)
                .deptIds(Collections.emptySet())
                .build();
    }

    static {
        // 匿名访问
        Anonymous = UserDetails.builder()
                .id(10L)
                .roles(Set.of(ANONYMOUS))
                .status(EntityStatus.ENABLE)
                .realName(ANONYMOUS.toLowerCase())
                .username(ANONYMOUS.toLowerCase())
                .userType(ANONYMOUS)
                .tenant(0L)
                .deptId(0L)
                .deptName(ANONYMOUS)
                .deptIds(Collections.emptySet())
                .build();
    }

    // ===== 基础信息 =====
    private Long id;
    private String username;
    private Long tenant;  // 租户 ID

    // ===== 权限信息 =====
    private Set<String> permissions;
    private Set<String> roles;

    // ===== 部门/组织信息 (数据权限) =====
    private Long deptId;              // 部门 ID
    private String deptName;          // 部门名称(可选,方便日志)
    private Set<Long> deptIds;        // 数据权限范围内的所有部门 ID

    // ===== Token  =====
    private String tokenId;           // Token 唯一标识(用于踢人/单点登录)
    private Long loginTime = System.currentTimeMillis();           // 登录时间戳
    private Long expireTime;          // 过期时间戳

    // ===== 用户状态  =====
    private String realName;          // 真实姓名(用于日志/审计)
    private String userType;          // 用户类型: EMPLOYEE, ADMIN, SYSTEM 等
    private EntityStatus status;           // 用户状态

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
        return expireTime != null && System.currentTimeMillis() > expireTime;
    }
}