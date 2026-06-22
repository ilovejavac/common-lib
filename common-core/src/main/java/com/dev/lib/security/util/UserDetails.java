package com.dev.lib.security.util;

import com.dev.lib.security.model.UserStatus;
import com.dev.lib.security.model.UserType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class UserDetails implements Serializable {

	@Serial
	private static final long serialVersionUID = 1L;

	private static final String INTERNAL = "INTERNAL";

	private static final String ANONYMOUS = "ANONYMOUS";

	private static final String SYSTEM = "SYSTEM";

	private static final Long ANONYMOUS_USER_ID = 1000L;

	private static final Long INTERNAL_USER_ID = 2000L;

	private static final Long SYSTEM_USER_ID = 3000L;

	public static final UserDetails Anonymous;

	public static final UserDetails Internal;

	public static final UserDetails System;

	static {
		Anonymous = UserDetails.builder()
				.id(ANONYMOUS_USER_ID)
				.roles(List.of(ANONYMOUS))
				.status(UserStatus.LOCKED)
				.username("anonymous")
				.tenant(ANONYMOUS_USER_ID)
				.deptId(ANONYMOUS_USER_ID)
				.validated(false)
				.build();

		Internal = UserDetails.builder()
				.id(INTERNAL_USER_ID)
				.roles(List.of(INTERNAL))
				.status(UserStatus.ACTIVE)
				.username("internal")
				.tenant(INTERNAL_USER_ID)
				.deptId(INTERNAL_USER_ID)
				.validated(true)
				.build();

		System = UserDetails.builder()
				.id(SYSTEM_USER_ID)
				.roles(List.of(SYSTEM))
				.status(UserStatus.ACTIVE)
				.username("system")
				.tenant(SYSTEM_USER_ID)
				.deptId(SYSTEM_USER_ID)
				.validated(true)
				.build();
	}

	private Boolean validated;

	// ===== 基础信息 =====
	private Long id;

	private String username;

	private Long tenant;  // 租户 ID

	// ===== 权限信息 =====
	private List<String> permissions;

	private List<String> roles;

	// ===== 部门/组织信息 (数据权限) =====
	private Long deptId;              // 部门 ID

	// ===== 用户状态  =====
	private UserStatus status;           // 用户状态

	// ===== 客户端信息 (审计/安全) =====
	private String clientIp;          // 客户端 IP

	private String clientType;        // 客户端类型: WEB, APP, MINI_PROGRAM

	private String deviceId;          // 设备 ID(可选)

	// ===== 工具方法 =====

	/**
	 * 是否超级管理员
	 */
	public boolean isSuperAdmin() {

		return hasRole("admin");
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