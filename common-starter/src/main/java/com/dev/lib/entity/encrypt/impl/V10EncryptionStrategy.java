package com.dev.lib.entity.encrypt.impl;

import com.dev.lib.entity.encrypt.EncryptService;
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
public class V10EncryptionStrategy implements EncryptService {

    @Resource
    private CustomEncryptor customEncryptor;

    @Override
    public String getVersion() {
        return "v10";
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