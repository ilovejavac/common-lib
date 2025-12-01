package com.dev.lib.security.util;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public class SecurityContextHolder {
    private static final ThreadLocal<UserDetails> holder = new ThreadLocal<>();

    private SecurityContextHolder() {
    }

    // ===== 基础方法 =====

    public static void set(UserDetails context) {
        holder.set(context);
    }

    public static void clear() {
        holder.remove();
    }

    public static UserDetails get() {
        return holder.get();
    }

    // ===== 便捷方法 =====
    public static boolean isLogin() {
        return holder.get() != null;
    }

    public static boolean validated() {
        return isLogin() && holder.get().getValidated();
    }

    public static void with(UserDetails userDetails, Runnable task) {
        UserDetails older = holder.get();
        holder.set(userDetails);
        try {
            task.run();
        } finally {
            holder.set(older);
        }
    }

    public static void withSystem(Runnable task) {
        with(UserDetails.System, task);
    }

    public static void withInternal(Runnable task) {
        with(UserDetails.Internal, task);
    }

    public static void withAnonymous(Runnable task) {
        with(UserDetails.Anonymous, task);
    }

    /**
     * 获取当前用户(未登录返回 Anonymous)
     */
    public static UserDetails current() {
        return Optional.ofNullable(holder.get())
                .orElse(UserDetails.Anonymous);
    }

    /**
     * 是否已登录真实用户
     */
    public static boolean isAuthenticated() {
        return current().isRealUser();
    }

    /**
     * 是否匿名用户
     */
    public static boolean isAnonymous() {
        return current().isAnonymous();
    }

    /**
     * 是否内部用户
     */
    public static boolean isInternal() {
        return current().isInternal();
    }

    // ===== 用户信息获取 =====

    /**
     * 获取用户ID(匿名返回 -1L)
     */
    public static Long getUserId() {
        return current().getId();
    }

    /**
     * 获取真实用户ID(匿名返回 null)
     */
    public static Long getUserIdOrNull() {
        UserDetails user = current();
        return user.isRealUser() ? user.getId() : null;
    }

    /**
     * 获取用户ID或抛出异常
     */
    public static Long getUserIdOrThrow() {
        UserDetails user = current();
        if (!user.isRealUser()) {
            throw new IllegalStateException("需要登录");
        }
        return user.getId();
    }

    /**
     * 获取用户名(匿名返回 "anonymous")
     */
    public static String getUsername() {
        return current().getUsername();
    }

    /**
     * 获取真实用户名(匿名返回 null)
     */
    public static String getUsernameOrNull() {
        UserDetails user = current();
        return user.isRealUser() ? user.getUsername() : null;
    }

    /**
     * 获取租户ID
     */
    public static Long getTenantId() {
        return current().getTenant();
    }

    /**
     * 获取部门ID
     */
    public static Long getDeptId() {
        return current().getDeptId();
    }

    // ===== 权限方法 =====

    public static List<String> getPermissions() {
        return Optional.ofNullable(current().getPermissions())
                .orElse(Collections.emptyList());
    }

    public static List<String> getRoles() {
        return Optional.ofNullable(current().getRoles())
                .orElse(Collections.emptyList());
    }

    /**
     * 是否有指定权限
     */
    public static boolean hasPermission(String permission) {
        return current().hasPermission(permission);
    }

    /**
     * 是否有指定角色
     */
    public static boolean hasRole(String role) {
        return current().hasRole(role);
    }

    /**
     * 是否超级管理员
     */
    public static boolean isSuperAdmin() {
        return current().isSuperAdmin();
    }
}