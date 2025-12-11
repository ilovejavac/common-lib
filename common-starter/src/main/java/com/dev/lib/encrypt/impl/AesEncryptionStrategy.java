package com.dev.lib.encrypt.impl;

import com.dev.lib.config.properties.AppSecurityProperties;
import com.dev.lib.encrypt.Encryptor;
import com.dev.lib.entity.encrypt.EncryptVersion;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * AES加密
 */
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.security", name = "encrypt-version", havingValue = "aes")
public class AesEncryptionStrategy implements Encryptor, InitializingBean {

    private final AppSecurityProperties securityProperties;

    private static final String ALGORITHM = "AES";

    private byte[] secretKeyBytes;

    @Override
    public void afterPropertiesSet() throws Exception {

        secretKeyBytes = securityProperties.getSecret().substring(
                0,
                16
        ).getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public String getVersion() {

        return EncryptVersion.AES.getMsg();
    }

    @Override
    public String encrypt(String plainText) {

        try {
            SecretKeySpec keySpec =
                    new SecretKeySpec(
                            secretKeyBytes,
                            ALGORITHM
                    );
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(
                    Cipher.ENCRYPT_MODE,
                    keySpec
            );
            byte[] encrypted = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(encrypted);
        } catch (Exception e) {
            throw new RuntimeException(
                    "V2 encryption failed",
                    e
            );
        }
    }

    @Override
    public String decrypt(String cipherText) {

        try {
            SecretKeySpec keySpec = new SecretKeySpec(
                    secretKeyBytes,
                    ALGORITHM
            );
            Cipher        cipher  = Cipher.getInstance(ALGORITHM);
            cipher.init(
                    Cipher.DECRYPT_MODE,
                    keySpec
            );
            byte[] decoded = Base64.getDecoder().decode(cipherText);
            byte[] decrypted = cipher.doFinal(decoded);
            return new String(
                    decrypted,
                    StandardCharsets.UTF_8
            );
        } catch (Exception e) {
            throw new RuntimeException(
                    "V2 decryption failed",
                    e
            );
        }
    }

}