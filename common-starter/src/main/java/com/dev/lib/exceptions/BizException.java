package com.dev.lib.exceptions;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class BizException extends RuntimeException {
    private final Integer coder;
    private final String msger;

    public BizException(Integer code, String message) {
        super(message);
        this.coder = code;
        this.msger = message;
    }
}
