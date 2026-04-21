package com.dev.lib.security.model;

import com.dev.lib.web.model.CodeEnums;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum UserType implements CodeEnums {
    ORDINARY_USER(10, "普通用户"),
    SYSTEM_ADMINISTRATOR(20, "系统管理员")
    ;
    private final Integer code;
    private final String message;
}
