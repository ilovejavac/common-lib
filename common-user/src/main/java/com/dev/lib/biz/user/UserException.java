package com.dev.lib.biz.user;

import com.dev.lib.exceptions.BizException;

public class UserException extends BizException {
    public UserException(UserError ce) {
        super(ce);
    }
}
