package com.dev.lib.entity.encrypt.impl;

import com.dev.lib.entity.encrypt.EncryptService;
import org.springframework.stereotype.Component;

import java.util.Base64;

/**
 * base64加密
 */
@Component
public class V1EncryptionStrategy implements EncryptService {
    @Override
    public String getVersion() {
        return "v1";
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
