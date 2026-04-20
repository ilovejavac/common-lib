package com.dev.lib.biz.bootstrap;

import com.dev.lib.web.model.CodeEnums;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum SystemBootstrapState implements CodeEnums {
    uninitialize(0, "未初始化"),
    initialized(1, "已初始化")
    ;

    private final Integer code;
    private final String message;
}
