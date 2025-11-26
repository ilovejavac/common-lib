package com.dev.lib.exceptions;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class BizException extends RuntimeException {
    private final Integer coder;
    private final String msger;
    private Object[] args;
    private boolean i18n = false;

    public BizException(Integer code, String message) {
        super(message);
        this.coder = code;
        this.msger = message;
    }

    /**
     * 标记为 i18n，需要翻译
     */
    public BizException i18n(Object... args) {
        this.i18n = true;
        this.args = args.length > 0 ? args : null;
        return this;
    }
}