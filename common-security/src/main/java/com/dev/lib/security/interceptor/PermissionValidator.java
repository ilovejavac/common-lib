package com.dev.lib.security.interceptor;

import com.dev.lib.config.properties.AppSecurityProperties;
import com.dev.lib.exceptions.BizException;
import com.dev.lib.security.config.properties.SecurityValidProperties;
import com.dev.lib.security.model.UserStatus;
import com.dev.lib.security.service.annotation.Anonymous;
import com.dev.lib.security.service.annotation.RequirePermission;
import com.dev.lib.security.service.annotation.RequireRole;
import com.dev.lib.security.util.SecurityContextHolder;
import com.dev.lib.security.util.UserDetails;
import com.dev.lib.web.model.StandardErrorCodes;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.PathMatcher;
import org.springframework.web.method.HandlerMethod;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class PermissionValidator implements InitializingBean {

	private static final String UNAUTHORIZED_MESSAGE = "Unauthorized";

	private static final String INVALID_SESSION_MESSAGE = "Invalid Session";

	private static final String ACCESS_DENIED_MESSAGE = "Access Denied";

	private final SecurityValidProperties validProperties;

	public boolean anonymous(HandlerMethod handlerMethod) {

		Class<?> controllerClass = handlerMethod.getBeanType();

		// 2. 方法级别 @Anonymous 优先检查
		if (handlerMethod.hasMethodAnnotation(Anonymous.class)) {
			anonymous();
			return true;
		}

		// 3. 类级别 @Anonymous
		if (controllerClass.isAnnotationPresent(Anonymous.class)) {
			anonymous();
			return true;
		}

		return false;
	}

	public void valid(HandlerMethod handlerMethod) {

		Class<?> controllerClass = handlerMethod.getBeanType();

		// 6. 必须登录
		requireActiveLogin();

		// 7. 方法级别 @RequireRole
		RequireRole methodRole = handlerMethod.getMethodAnnotation(RequireRole.class);
		if (methodRole != null) {
			if (!SecurityContextHolder.hasRole(methodRole.value())) {
				throw new BizException(StandardErrorCodes.PERMISSION_DENIED, ACCESS_DENIED_MESSAGE);
			}
			return;
		}

		// 8. 类级别 @RequireRole
		RequireRole classRole = controllerClass.getAnnotation(RequireRole.class);
		if (classRole != null && !SecurityContextHolder.hasRole(classRole.value())) {
			throw new BizException(StandardErrorCodes.PERMISSION_DENIED, ACCESS_DENIED_MESSAGE);
		}

		// 9. 方法级别 @RequirePermission
		RequirePermission methodPermission = handlerMethod.getMethodAnnotation(RequirePermission.class);
		if (methodPermission != null) {
			if (!SecurityContextHolder.hasPermission(methodPermission.value())) {
				throw new BizException(StandardErrorCodes.PERMISSION_DENIED, ACCESS_DENIED_MESSAGE);
			}
			return;
		}

		// 10. 类级别 @RequirePermission
		RequirePermission classPermission = controllerClass.getAnnotation(RequirePermission.class);
		if (classPermission != null && !SecurityContextHolder.hasPermission(classPermission.value())) {
			throw new BizException(StandardErrorCodes.PERMISSION_DENIED, ACCESS_DENIED_MESSAGE);
		}
	}

	private void requireActiveLogin() {

		UserDetails userDetails = SecurityContextHolder.get();
		if (userDetails == null || UserDetails.Anonymous.equals(userDetails)) {
			throw new BizException(StandardErrorCodes.AUTHENTICATION_FAILED, UNAUTHORIZED_MESSAGE);
		}
		if (!Boolean.TRUE.equals(userDetails.getValidated())) {
			throw new BizException(StandardErrorCodes.AUTHENTICATION_FAILED, INVALID_SESSION_MESSAGE);
		}
		if (userDetails.getStatus() != null && !UserStatus.ACTIVE.equals(userDetails.getStatus())) {
			throw new BizException(StandardErrorCodes.PERMISSION_DENIED, accountStatusMessage(userDetails.getStatus()));
		}
	}

	private String accountStatusMessage(UserStatus status) {

		if (UserStatus.DISABLED.equals(status)) {
			return "Account Disabled";
		}
		if (UserStatus.LOCKED.equals(status)) {
			return "Account Locked";
		}
		if (UserStatus.VERIFING.equals(status)) {
			return "Pending Verification";
		}
		return ACCESS_DENIED_MESSAGE;
	}

	private void anonymous() {

		if (!SecurityContextHolder.isLogin()) {
			SecurityContextHolder.set(UserDetails.Anonymous);
		}
	}

	public boolean shouldSkip(HttpServletRequest request) {

		// 可能被 internal 认证过了
		if (SecurityContextHolder.isInternal()) {
			return true;
		}
		return isWhitelistRequest(request.getRequestURI());
	}

	private final AppSecurityProperties securityProperties;

	// 以下接口放行
	private final Set<String> whitelistPatterns = new HashSet<>(Arrays.asList("/healthz", "/api/login", "/api/register", "/api/public/**", "/api/bootstrap/**"));

	private final PathMatcher pathMatcher = new AntPathMatcher();

	private boolean isWhitelistRequest(String uri) {

		return whitelistPatterns.stream().anyMatch(pattern -> pathMatcher.match(pattern, uri));
	}

	@Override
	public void afterPropertiesSet() {

		if (securityProperties.getWhiteListRequest() != null) {
			whitelistPatterns.addAll(securityProperties.getWhiteListRequest());
		}

		if (validProperties.getSkipPatterns() != null) {
			whitelistPatterns.addAll(validProperties.getSkipPatterns());
		}
	}

}
