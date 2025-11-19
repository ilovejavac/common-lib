package com.dev.lib.entity.encrypt;

public interface CustomEncryptor {
    String doEncrypt(String value);

    String doDecrypt(String value);
}
