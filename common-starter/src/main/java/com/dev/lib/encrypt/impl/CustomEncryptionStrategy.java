package com.dev.lib.encrypt.impl;

import com.dev.lib.encrypt.CustomEncryptor;
import com.dev.lib.encrypt.Encryptor;
import com.dev.lib.entity.encrypt.EncryptVersion;
import jakarta.annotation.Resource;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * 自定义加密
 */
@Component
@RequiredArgsConstructor
@ConditionalOnBean(CustomEncryptor.class)
@ConditionalOnProperty(prefix = "app.security", name = "encrypt-version", havingValue = "custom")
public class CustomEncryptionStrategy implements Encryptor {

    @Resource
    private CustomEncryptor customEncryptor;

    @Override
    public String getVersion() {
        return EncryptVersion.CUSTOM.getMsg();
    }

    @Override
    public String encrypt(String plainText) {
        return customEncryptor.doEncrypt(plainText);
    }

    @Override
    public String decrypt(String cipherText) {
        return customEncryptor.doDecrypt(cipherText);
    }
}