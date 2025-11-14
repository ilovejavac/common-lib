package com.dev.lib.entity.encrypt.impl;

public interface CustomEncryptor {
    String doEncrypt(String value);

    String doDecrypt(String value);
}
