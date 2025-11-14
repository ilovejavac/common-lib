package com.dev.lib.security.util;

import java.util.Collections;
import java.util.Optional;
import java.util.Set;

public class SecurityContextHolder {
    private static final ThreadLocal<UserDetails> holder = new ThreadLocal<>();

    public static boolean isLogin() {
        return holder.get() != null;
    }

    public static void set(UserDetails context) {
        holder.set(context);
    }

    public static void clear() {
        holder.remove();
    }

    public static UserDetails get() {
        return holder.get();
    }

    private SecurityContextHolder() {
    }

    public static Long getUserId() {
        return Optional.ofNullable(holder.get())
                .map(UserDetails::getId)
                .orElse(null);
    }

    public static String getUsername() {
        return Optional.ofNullable(holder.get())
                .map(UserDetails::getUsername)
                .orElse(null);
    }

    public static Set<String> getPermissions() {
        return Optional.ofNullable(holder.get())
                .map(UserDetails::getPermissions)
                .orElse(Collections.emptySet());
    }

    public static Set<String> getRoles() {
        return Optional.ofNullable(holder.get())
                .map(UserDetails::getRoles)
                .orElse(Collections.emptySet());
    }
}