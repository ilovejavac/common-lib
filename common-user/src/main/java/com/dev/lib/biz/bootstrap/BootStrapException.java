package com.dev.lib.biz.bootstrap;

import com.dev.lib.exceptions.BizException;
import lombok.ToString;

@ToString
public class BootStrapException extends BizException {
    public BootStrapException(BootstrapError error) {
        super(error);
    }
}
