package com.dev.lib.biz.user;

import com.dev.lib.web.model.CodeEnums;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum UserStatus implements CodeEnums {

    ;
    private final Integer code;
    private final String message;
}
