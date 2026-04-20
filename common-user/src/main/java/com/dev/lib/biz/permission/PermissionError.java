package com.dev.lib.biz.permission;

import com.dev.lib.web.model.CodeEnums;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum PermissionError implements CodeEnums {

    ;
    private final Integer code;
    private final String message;
}
