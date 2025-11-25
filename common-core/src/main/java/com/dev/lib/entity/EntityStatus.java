package com.dev.lib.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum EntityStatus {
    ENABLE(0, "允许"),
    DISABLE(1, "禁用");
    private final Integer code;
    private final String msg;
}
