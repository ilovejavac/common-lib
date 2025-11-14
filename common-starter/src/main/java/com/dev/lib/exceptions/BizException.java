package com.dev.lib.exceptions;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class BizException extends RuntimeException {
    private final Integer code;
    private final String msg;

    public BizException(Integer code, String message) {
        super(message);
        this.code = code;
        this.msg = message;
    }
}
