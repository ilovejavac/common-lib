package com.dev.lib.biz.bootstrap;

import com.dev.lib.web.model.CodeEnums;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum BootstrapError implements CodeEnums {
    SYSTEM_NOT_INITIALIZE(0, "系统未初始化"),
    SYSTEM_HAS_BEEN_INITIALIZED(1, "系统已初始化")
    ;

    private final Integer code;
    private final String message;
}
