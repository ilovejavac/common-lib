package com.dev.lib.entity.encrypt;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum EncryptVersion {
    V1("Base64"),
    V2("AES"),
    V3("SM4"),

    V10("custom");

    private final String msg;
}
