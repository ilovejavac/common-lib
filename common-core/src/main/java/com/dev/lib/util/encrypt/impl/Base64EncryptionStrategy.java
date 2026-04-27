package com.dev.lib.util.encrypt.impl;

import com.dev.lib.entity.encrypt.EncryptVersion;
import com.dev.lib.util.encrypt.Encryptor;
import com.dev.lib.util.encrypt.condition.ConditionalOnEncryptionStrategy;
import org.springframework.stereotype.Component;

import java.util.Base64;

/**
 * base64加密
 */
@Component
@ConditionalOnEncryptionStrategy(EncryptVersion.BASE64)
public class Base64EncryptionStrategy implements Encryptor {

    @Override
    public EncryptVersion getVersion() {

        return EncryptVersion.BASE64;
    }

    @Override
    public String encrypt(String plainText) {

        return Base64.getEncoder().encodeToString(plainText.getBytes());
    }

    @Override
    public String decrypt(String cipherText) {

        return new String(Base64.getDecoder().decode(cipherText));
    }

}
