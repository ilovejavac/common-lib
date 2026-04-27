package com.dev.lib.util.encrypt.impl;

import com.dev.lib.config.properties.AppSecurityProperties;
import com.dev.lib.entity.encrypt.EncryptVersion;
import com.dev.lib.util.encrypt.Encryptor;
import com.dev.lib.util.encrypt.condition.ConditionalOnEncryptionStrategy;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * AES加密
 */
@Component
@RequiredArgsConstructor
// openssl rand -base64 32
@ConditionalOnEncryptionStrategy(EncryptVersion.AES)
public class AesGcmEncryptionStrategy implements Encryptor, InitializingBean {

    private final AppSecurityProperties securityProperties;

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_TAG_LENGTH = 128;
    private static final int IV_LENGTH = 12;
    private static final int KEY_LENGTH = 256; // bits

    private SecretKeySpec secretKey;

    @Override
    public void afterPropertiesSet() {
        byte[] keyBytes = loadBase64Key(securityProperties.getAes().getSecret());
        this.secretKey = new SecretKeySpec(keyBytes, "AES");
    }

    @Override
    public EncryptVersion getVersion() {

        return EncryptVersion.AES;
    }

    @Override
    public String encrypt(String plainText) {
        try {
            // ✅ 每次使用随机IV
            byte[]       iv     = new byte[IV_LENGTH];
            SecureRandom random = new SecureRandom();
            random.nextBytes(iv);

            GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH,
                                                            iv);
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, gcmSpec);

            byte[] encrypted = cipher.doFinal(
                    plainText.getBytes(StandardCharsets.UTF_8)
            );

            // ✅ IV不需要保密,但必须和密文一起存储
            byte[] combined = new byte[IV_LENGTH + encrypted.length];
            System.arraycopy(iv, 0, combined, 0, IV_LENGTH);
            System.arraycopy(encrypted, 0, combined, IV_LENGTH,
                             encrypted.length);

            return Base64.getEncoder().encodeToString(combined);
        } catch (Exception e) {
            throw new RuntimeException("Encryption failed", e);
        }
    }

    @Override
    public String decrypt(String cipherText) {
        try {
            byte[] combined = Base64.getDecoder().decode(cipherText);

            // ✅ 提取IV
            byte[] iv = new byte[IV_LENGTH];
            System.arraycopy(combined, 0, iv, 0, IV_LENGTH);

            byte[] encrypted = new byte[combined.length - IV_LENGTH];
            System.arraycopy(combined, IV_LENGTH, encrypted, 0,
                             encrypted.length);

            GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH,
                                                            iv);
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmSpec);

            byte[] decrypted = cipher.doFinal(encrypted);
            return new String(decrypted, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("Decryption failed", e);
        }
    }

    private byte[] loadBase64Key(String encodedKey) {
        if (encodedKey == null || encodedKey.isBlank()) {
            throw new IllegalStateException(
                    "app.security.aes.secret must be a Base64-encoded 32-byte AES key"
            );
        }

        try {
            byte[] keyBytes = Base64.getDecoder().decode(encodedKey);
            if (keyBytes.length != KEY_LENGTH / 8) {
                throw new IllegalStateException(
                        "app.security.aes.secret must decode to exactly 32 bytes for AES-256"
                );
            }
            return keyBytes;
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException(
                    "app.security.aes.secret must be valid Base64",
                    e
            );
        }
    }

}
