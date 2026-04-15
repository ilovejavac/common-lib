package com.dev.lib.util.encrypt;

public interface Encryptor {

    String getVersion();

    String encrypt(String value);

    String decrypt(String value);

}
