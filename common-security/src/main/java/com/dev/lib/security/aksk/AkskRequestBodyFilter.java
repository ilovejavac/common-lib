package com.dev.lib.security.aksk;

import com.dev.lib.aksk.config.AkskProperties;

/**
 * @deprecated Use {@link com.dev.lib.aksk.web.AkskRequestBodyFilter}.
 */
@Deprecated(since = "1.5.1", forRemoval = true)
public class AkskRequestBodyFilter extends com.dev.lib.aksk.web.AkskRequestBodyFilter {

    public AkskRequestBodyFilter(AkskProperties properties) {

        super(properties);
    }
}
