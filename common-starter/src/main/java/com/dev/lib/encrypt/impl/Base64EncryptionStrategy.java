package com.dev.lib.encrypt.impl;

import com.dev.lib.encrypt.Encryptor;
import com.dev.lib.entity.encrypt.EncryptVersion;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Base64;

/**
 * base64加密
 */
@Component
@ConditionalOnProperty(prefix = "app.security", name = "encrypt-version", havingValue = "base64")
public class Base64EncryptionStrategy implements Encryptor {

    @Override
    public String getVersion() {

        return EncryptVersion.BASE64.getMsg();
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
