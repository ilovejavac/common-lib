package com.dev.lib.util.encrypt;

public interface CustomEncryptor {

    String doEncrypt(String value);

    String doDecrypt(String value);

}
