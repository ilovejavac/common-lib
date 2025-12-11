package com.dev.lib.entity.encrypt;

public interface EncryptionService {

    String encrypt(String dbValue);

    String decrypt(String dbValue);

}
