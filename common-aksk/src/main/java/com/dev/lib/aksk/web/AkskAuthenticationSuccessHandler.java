package com.dev.lib.aksk.web;

import com.dev.lib.aksk.domain.model.AkskAuthentication;
import jakarta.servlet.http.HttpServletRequest;

@FunctionalInterface
public interface AkskAuthenticationSuccessHandler {

    void onSuccess(AkskAuthentication authentication, HttpServletRequest request);
}
