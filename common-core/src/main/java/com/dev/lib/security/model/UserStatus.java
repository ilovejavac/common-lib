package com.dev.lib.security.model;

import com.dev.lib.web.model.CodeEnums;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum UserStatus implements CodeEnums {
    ACTIVE(10, "激活"),
    LOCKED(20, "锁定"),
    DISABLED(30, "禁用"),
    VERIFING(40, "待验证")
    ;

    private final Integer code;
    private final String message;
}
