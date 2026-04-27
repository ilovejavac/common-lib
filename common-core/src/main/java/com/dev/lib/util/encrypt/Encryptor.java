package com.dev.lib.util.encrypt;

import com.dev.lib.entity.encrypt.EncryptVersion;

public interface Encryptor {

    EncryptVersion getVersion();

    String encrypt(String value);

    String decrypt(String value);

}
