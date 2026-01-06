package com.dev.lib.security;

import com.dev.lib.exceptions.BizException;

public class TokenException extends BizException {

    public TokenException(String message) {

        super(104001, message);
    }

}
