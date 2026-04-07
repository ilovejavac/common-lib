package com.dev.lib.encrypt.impl;

import com.dev.lib.config.properties.AppSecurityProperties;
import org.junit.jupiter.api.Test;

import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AesGcmEncryptionStrategyTest {

    @Test
    void shouldEncryptAndDecryptUsingBase64Encoded32ByteKey() {

        AppSecurityProperties properties = new AppSecurityProperties();
        properties.setSecret(Base64.getEncoder().encodeToString(new byte[32]));

        AesGcmEncryptionStrategy strategy = new AesGcmEncryptionStrategy(properties);
        strategy.afterPropertiesSet();

        String plainText = "hello-aes-gcm";

        String cipherText = strategy.encrypt(plainText);

        assertEquals(plainText, strategy.decrypt(cipherText));
    }

    @Test
    void shouldPrefixCiphertextWith12ByteIv() {

        AppSecurityProperties properties = new AppSecurityProperties();
        properties.setSecret(Base64.getEncoder().encodeToString(new byte[32]));

        AesGcmEncryptionStrategy strategy = new AesGcmEncryptionStrategy(properties);
        strategy.afterPropertiesSet();

        String plainText = "iv-check";
        byte[] combined = Base64.getDecoder().decode(strategy.encrypt(plainText));

        assertEquals(12 + plainText.length() + 16, combined.length);
    }

    @Test
    void shouldRejectSecretWhenDecodedKeyLengthIsNot32Bytes() {

        AppSecurityProperties properties = new AppSecurityProperties();
        properties.setSecret(Base64.getEncoder().encodeToString(new byte[16]));

        AesGcmEncryptionStrategy strategy = new AesGcmEncryptionStrategy(properties);

        assertThrows(IllegalStateException.class, strategy::afterPropertiesSet);
    }
}
