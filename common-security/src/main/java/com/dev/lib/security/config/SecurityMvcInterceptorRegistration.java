package com.dev.lib.security.config;

import com.dev.lib.security.interceptor.AuthInterceptor;
import com.dev.lib.security.interceptor.InternalInterceptor;
import com.dev.lib.web.interceptor.CommonMvcInterceptorRegistration;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;

@Component
@RequiredArgsConstructor
public class SecurityMvcInterceptorRegistration implements CommonMvcInterceptorRegistration {

	private final AuthInterceptor authInterceptor;

	private final InternalInterceptor internalInterceptor;

	@Override
	public void addInterceptors(InterceptorRegistry registry) {

		registry.addInterceptor(internalInterceptor)
				.order(10)
				.addPathPatterns("/**");

		registry.addInterceptor(authInterceptor)
				.order(20)
				.addPathPatterns("/**");
	}

}
