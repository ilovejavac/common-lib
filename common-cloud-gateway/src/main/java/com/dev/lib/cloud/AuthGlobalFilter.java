package com.dev.lib.cloud;

import com.dev.lib.security.filter.AuthTokenFilter;
import com.dev.lib.security.web.UserContextHeaders;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.NonNull;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;

@Component
public class AuthGlobalFilter extends OncePerRequestFilter implements Ordered {

	@Override
	protected void doFilterInternal(@NonNull HttpServletRequest request,
	                                @NonNull HttpServletResponse response,
	                                @NonNull FilterChain filterChain) throws ServletException, IOException {

		HeaderRewriteRequestWrapper wrappedRequest = new HeaderRewriteRequestWrapper(
				request,
				UserContextHeaders.names(),
				Map.of()
		);
		filterChain.doFilter(wrappedRequest, response);
	}

	@Override
	public int getOrder() {

		return AuthTokenFilter.AUTH_TOKEN_FILTER_ORDER - 10;
	}

}
