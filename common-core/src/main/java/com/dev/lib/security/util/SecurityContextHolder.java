package com.dev.lib.security.util;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class SecurityContextHolder {

	private static final ScopedValue<SecurityContext> holder = ScopedValue.newInstance();

	private SecurityContextHolder() {

	}

	// ===== 基础方法 =====

	public static void set(UserDetails context) {

		currentContext().userDetails = context;
	}

	public static void clear() {

		if (holder.isBound()) {
			holder.get().userDetails = null;
		}
	}

	public static UserDetails get() {

		return holder.isBound() ? holder.get().userDetails : null;
	}

	public static <T> Optional<T> getValue(String key, Class<T> type) {

		UserDetails userDetails = get();

		if (userDetails == null || userDetails.getPayload() == null) {
			return Optional.empty();
		}

		Object value = userDetails.getPayload().get(key);
		if (value == null) {
			return Optional.empty();
		}

		if (type.isInstance(value)) {
			return Optional.of(type.cast(value));
		}

		if (type.isEnum() && value instanceof String enumName) {
			for (T constant : type.getEnumConstants()) {
				if (((Enum<?>) constant).name().equals(enumName)) {
					return Optional.of(constant);
				}
			}

			throw new IllegalArgumentException(
					"Invalid enum value '%s' for %s"
							.formatted(enumName, type.getSimpleName())
			);
		}

		return Optional.empty();
	}

	public static String getStr(String key) {

		return getValue(key, String.class).orElse(null);
	}

	// ===== 便捷方法 =====
	public static boolean isLogin() {

		UserDetails userDetails = get();
		return userDetails != null
				&& !UserDetails.Anonymous.equals(userDetails)
				&& Boolean.TRUE.equals(userDetails.getValidated());
	}

	public static boolean validated() {

		return isLogin();
	}

	public static void with(UserDetails userDetails, Runnable task) {

		ScopedValue.where(holder, new SecurityContext(userDetails)).run(task);
	}

	public static void withEmptyContext(Runnable task) {

		with(null, task);
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

		return Optional.ofNullable(get())
				.orElse(UserDetails.Anonymous);
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
	 * 获取用户名(匿名返回 "anonymous")
	 */
	public static String getUsername() {

		return current().getUsername();
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
	public static boolean hasPermission(String... permissions) {

		return Arrays.stream(permissions).allMatch(current()::hasPermission);
	}

	/**
	 * 是否有指定角色
	 */
	public static boolean hasRole(String... roles) {

		return Arrays.stream(roles).allMatch(current()::hasRole);
	}

	/**
	 * 是否超级管理员
	 */
	public static boolean isSuperAdmin() {

		return current().isSuperAdmin();
	}

	private static SecurityContext currentContext() {

		if (!holder.isBound()) {
			throw new IllegalStateException("SecurityContextHolder requires an active ScopedValue context");
		}
		return holder.get();
	}

	private static class SecurityContext {

		private UserDetails userDetails;

		private SecurityContext(UserDetails userDetails) {

			this.userDetails = userDetails;
		}

	}

}
