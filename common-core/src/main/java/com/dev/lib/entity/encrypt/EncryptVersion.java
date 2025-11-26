package com.dev.lib.entity.encrypt;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum EncryptVersion {
    BASE64("v1"),
    AES("v2"),

    RSA("v3"),
    SM4("v4"),

    CUSTOM("v10");

    private final String msg;

    public static EncryptVersion from(String msg) {
        for (EncryptVersion value : values()) {
            if (value.msg.equals(msg)) {
                return value;
            }
        }

        return EncryptVersion.BASE64;
    }
}
