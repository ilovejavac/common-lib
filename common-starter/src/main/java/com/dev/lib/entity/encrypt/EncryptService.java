package com.dev.lib.entity.encrypt;

public interface EncryptService {
    String getVersion();

    String encrypt(String value);

    String decrypt(String value);
}
