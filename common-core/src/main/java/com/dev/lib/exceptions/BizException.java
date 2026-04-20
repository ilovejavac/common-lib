package com.dev.lib.exceptions;

import com.dev.lib.web.model.CodeEnums;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class BizException extends RuntimeException {

    private final Integer coder;

    private final String msger;

    private Object[] args;

    private boolean i18n = false;

    public BizException(CodeEnums ce) {
        super(ce.getMessage());
        this.coder = ce.getCode();
        this.msger = ce.getMessage();
    }

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

    public static <E extends CodeEnums> void notNull(Object o, E e) {

        if (o == null) {
            throw new BizException(e.getCode(), e.getMessage());
        }
    }

}