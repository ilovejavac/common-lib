package com.dev.lib.encrypt;

public interface CustomEncryptor {
    String doEncrypt(String value);

    String doDecrypt(String value);
}
