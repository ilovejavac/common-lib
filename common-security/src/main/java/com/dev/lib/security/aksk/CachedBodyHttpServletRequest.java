package com.dev.lib.security.aksk;

import jakarta.servlet.http.HttpServletRequest;

/**
 * @deprecated Use {@link com.dev.lib.aksk.web.CachedBodyHttpServletRequest}.
 */
@Deprecated(since = "1.5.1", forRemoval = true)
public class CachedBodyHttpServletRequest extends com.dev.lib.aksk.web.CachedBodyHttpServletRequest {

    public CachedBodyHttpServletRequest(HttpServletRequest request, byte[] cachedBody) {

        super(request, cachedBody);
    }
}
