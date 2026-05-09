package com.dev.lib.security.interceptor;

import com.dev.lib.aksk.config.AkskProperties;
import com.dev.lib.aksk.domain.service.AkskHeaderResolver;
import com.dev.lib.aksk.domain.service.AkskVerifier;
import com.dev.lib.security.aksk.AkskSecurityContextAdapter;
import com.dev.lib.security.aksk.SecurityAkskAuthenticationSuccessHandler;

import java.util.List;

/**
 * @deprecated Use {@link com.dev.lib.aksk.web.AkskAuthenticationInterceptor}.
 */
@Deprecated(since = "1.5.1", forRemoval = true)
public class AkskAuthenticationInterceptor extends com.dev.lib.aksk.web.AkskAuthenticationInterceptor {

    public AkskAuthenticationInterceptor(
            AkskVerifier verifier,
            AkskHeaderResolver headerResolver,
            AkskProperties properties,
            AkskSecurityContextAdapter securityContextAdapter
    ) {

        super(
                verifier,
                headerResolver,
                properties,
                List.of(new SecurityAkskAuthenticationSuccessHandler(securityContextAdapter))
        );
    }
}
