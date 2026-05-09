package com.dev.lib.security.aksk;

import com.dev.lib.aksk.domain.model.AkskAuthentication;
import com.dev.lib.aksk.web.AkskAuthenticationSuccessHandler;
import com.dev.lib.security.util.SecurityContextHolder;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SecurityAkskAuthenticationSuccessHandler implements AkskAuthenticationSuccessHandler {

    private final AkskSecurityContextAdapter securityContextAdapter;

    @Override
    public void onSuccess(AkskAuthentication authentication, HttpServletRequest request) {

        SecurityContextHolder.set(securityContextAdapter.toUserDetails(authentication));
    }
}
