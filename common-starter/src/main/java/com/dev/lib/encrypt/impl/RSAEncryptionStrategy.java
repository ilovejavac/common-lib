package com.dev.lib.encrypt.impl;

import com.dev.lib.config.properties.AppSecurityProperties;
import com.dev.lib.encrypt.Encryptor;
import com.dev.lib.entity.encrypt.EncryptVersion;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

/**
 * RSA 加密
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.security", name = "encrypt-version", havingValue = "rsa")
public class RSAEncryptionStrategy implements Encryptor, InitializingBean {

    private final AppSecurityProperties securityProperties;

    private static final String ALGORITHM = "RSA";

    private static final int    KEY_SIZE  = 2048;

    private PublicKey  publicKey;

    private PrivateKey privateKey;

    @Override
    public void afterPropertiesSet() throws Exception {

        // 方案1: 从配置读取已有密钥对
        String publicKeyStr  = securityProperties.getRsaPublicKey();
        String privateKeyStr = securityProperties.getRsaPrivateKey();

        if (publicKeyStr != null && privateKeyStr != null) {
            // 解析已有密钥
            KeyFactory keyFactory = KeyFactory.getInstance(ALGORITHM);

            byte[]             publicKeyBytes = Base64.getDecoder().decode(publicKeyStr);
            X509EncodedKeySpec publicKeySpec  = new X509EncodedKeySpec(publicKeyBytes);
            this.publicKey = keyFactory.generatePublic(publicKeySpec);

            byte[]              privateKeyBytes = Base64.getDecoder().decode(privateKeyStr);
            PKCS8EncodedKeySpec privateKeySpec  = new PKCS8EncodedKeySpec(privateKeyBytes);
            this.privateKey = keyFactory.generatePrivate(privateKeySpec);
        } else {
            // 方案2: 动态生成密钥对(首次使用)
            KeyPairGenerator keyGen = KeyPairGenerator.getInstance(ALGORITHM);
            keyGen.initialize(KEY_SIZE);
            KeyPair keyPair = keyGen.generateKeyPair();
            this.publicKey = keyPair.getPublic();
            this.privateKey = keyPair.getPrivate();

            log.warn("未配置 RSA 密钥,已动态生成。请保存以下密钥:");
            log.warn(
                    "PublicKey: {}",
                    Base64.getEncoder().encodeToString(publicKey.getEncoded())
            );
            log.warn(
                    "PrivateKey: {}",
                    Base64.getEncoder().encodeToString(privateKey.getEncoded())
            );
        }
    }

    @Override
    public String getVersion() {

        return EncryptVersion.RSA.getMsg();
    }

    @Override
    public String encrypt(String plainText) {

        try {
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(
                    Cipher.ENCRYPT_MODE,
                    publicKey
            );
            byte[] encrypted = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(encrypted);
        } catch (Exception e) {
            throw new RuntimeException(
                    "RSA encryption failed",
                    e
            );
        }
    }

    @Override
    public String decrypt(String cipherText) {

        try {
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(
                    Cipher.DECRYPT_MODE,
                    privateKey
            );
            byte[] decoded   = Base64.getDecoder().decode(cipherText);
            byte[] decrypted = cipher.doFinal(decoded);
            return new String(
                    decrypted,
                    StandardCharsets.UTF_8
            );
        } catch (Exception e) {
            throw new RuntimeException(
                    "RSA decryption failed",
                    e
            );
        }
    }

}