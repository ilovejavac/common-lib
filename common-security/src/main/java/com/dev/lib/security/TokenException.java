package com.dev.lib.security;

import com.dev.lib.exceptions.BizException;
import com.dev.lib.web.model.StandardErrorCodes;

public class TokenException extends BizException {

    public TokenException(String message) {

        super(StandardErrorCodes.TOKEN_INVALID, message);
    }

}
