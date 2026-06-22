package com.dev.lib.security.filter;

import com.dev.lib.security.TokenException;
import com.dev.lib.security.service.TokenService;
import com.dev.lib.security.util.ClientInfoExtractor;
import com.dev.lib.security.util.SecurityContextHolder;
import com.dev.lib.security.util.UserDetails;
import com.dev.lib.security.web.UserContextHeaders;
import com.dev.lib.util.StringUtils;
import com.dev.lib.web.model.ServerResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuthTokenFilter extends OncePerRequestFilter implements Ordered {

	public static final int AUTH_TOKEN_FILTER_ORDER = 10001;

	private static final String AUTHORIZATION_HEADER = "Authorization";

	private static final String BEARER_PREFIX = "Bearer ";

	private final TokenService tokenService;

	@Override
	public int getOrder() {

		return AUTH_TOKEN_FILTER_ORDER;
	}

	@Override
	protected void doFilterInternal(
			@NonNull HttpServletRequest request,
			@NonNull HttpServletResponse response,
			@NonNull FilterChain filterChain
	                               ) throws ServletException, IOException {

		try {
			SecurityContextHolder.withAnonymous(() -> {
				try {
					setContextInfo(request);
				} catch (TokenException ex) {
					ServerResponse.fail(ex.getCoder(), ex.getMsger()).to(response);
					return;
				}

				try {
					filterChain.doFilter(request, response);
				} catch (IOException | ServletException ex) {
					throw new FilterChainException(ex);
				}
			});
		} catch (FilterChainException ex) {
			Throwable cause = ex.getCause();
			if (cause instanceof IOException ioException) {
				throw ioException;
			}
			if (cause instanceof ServletException servletException) {
				throw servletException;
			}
			throw ex;
		}
	}

	private String extractToken(HttpServletRequest request) {

		String bearerToken = request.getHeader(AUTHORIZATION_HEADER);
		return (bearerToken != null && bearerToken.startsWith(BEARER_PREFIX))
		       ? bearerToken.substring(BEARER_PREFIX.length())
		       : null;
	}

	private void setContextInfo(HttpServletRequest request) {

		UserDetails headerUser = UserContextHeaders.from(request).orElse(null);
		if (headerUser != null) {
			fillMissingClientInfo(request, headerUser);
			SecurityContextHolder.set(headerUser);
			return;
		}

		String token = extractToken(request);
		if (StringUtils.isBlank(token)) {
			return;
		}
		UserDetails userDetail = tokenService.parseToken(token);
		if (userDetail != null) {
			setContextUser(request, userDetail);
		}
	}

	private void setContextUser(HttpServletRequest request, UserDetails userDetail) {

		userDetail.setClientIp(ClientInfoExtractor.getClientIp(request));
		userDetail.setClientType(ClientInfoExtractor.getClientType(request));
		userDetail.setDeviceId(request.getHeader("X-Device-Id"));

		SecurityContextHolder.set(userDetail);
	}

	private void fillMissingClientInfo(HttpServletRequest request, UserDetails userDetail) {

		if (StringUtils.isBlank(userDetail.getClientIp())) {
			userDetail.setClientIp(ClientInfoExtractor.getClientIp(request));
		}
		if (StringUtils.isBlank(userDetail.getClientType())) {
			userDetail.setClientType(ClientInfoExtractor.getClientType(request));
		}
		if (StringUtils.isBlank(userDetail.getDeviceId())) {
			userDetail.setDeviceId(request.getHeader("X-Device-Id"));
		}
	}

	private static class FilterChainException extends RuntimeException {

		private FilterChainException(Exception cause) {

			super(cause);
		}

	}

}
