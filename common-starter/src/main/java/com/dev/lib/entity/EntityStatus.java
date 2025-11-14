package com.dev.lib.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum EntityStatus {
    ENABLE(0, ""),
    DISABLE(1, "");
    private final Integer code;
    private final String msg;
}
