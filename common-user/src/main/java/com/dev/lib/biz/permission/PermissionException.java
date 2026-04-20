package com.dev.lib.biz.permission;

import com.dev.lib.exceptions.BizException;

public class PermissionException extends BizException {
    public PermissionException(PermissionError ce) {
        super(ce);
    }
}
