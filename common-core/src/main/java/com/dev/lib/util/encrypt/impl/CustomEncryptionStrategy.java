package com.dev.lib.util.encrypt.impl;

import com.dev.lib.entity.encrypt.EncryptVersion;
import com.dev.lib.util.encrypt.CustomEncryptor;
import com.dev.lib.util.encrypt.Encryptor;
import com.dev.lib.util.encrypt.condition.ConditionalOnEncryptionStrategy;
import jakarta.annotation.Resource;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;

/**
 * 自定义加密
 */
@Component
@RequiredArgsConstructor
@ConditionalOnBean(CustomEncryptor.class)
@ConditionalOnEncryptionStrategy(EncryptVersion.CUSTOM)
public class CustomEncryptionStrategy implements Encryptor {

    @Resource
    private CustomEncryptor customEncryptor;

    @Override
    public EncryptVersion getVersion() {

        return EncryptVersion.CUSTOM;
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
