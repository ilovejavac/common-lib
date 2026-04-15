package com.dev.lib.util.encrypt.impl;

import com.dev.lib.config.properties.AppSecurityProperties;
import org.junit.jupiter.api.Test;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RSAEncryptionStrategyTest {

    @Test
    void shouldEncryptUsingSha256OaepAndDecryptSuccessfully() throws Exception {

        KeyPair keyPair = KeyPairGenerator.getInstance("RSA").generateKeyPair();

        AppSecurityProperties properties = new AppSecurityProperties();
        properties.setRsaPublicKey(
                Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded())
        );
        properties.setRsaPrivateKey(
                Base64.getEncoder().encodeToString(keyPair.getPrivate().getEncoded())
        );

        RSAEncryptionStrategy strategy = new RSAEncryptionStrategy(properties);
        strategy.afterPropertiesSet();

        String plainText = "hello-rsa";
        String cipherText = strategy.encrypt(plainText);

        assertEquals(plainText, strategy.decrypt(cipherText));

        byte[] decodedCipherText = Base64.getDecoder().decode(cipherText);

        Cipher legacyCipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        legacyCipher.init(Cipher.DECRYPT_MODE, keyPair.getPrivate());

        assertThrows(BadPaddingException.class, () -> legacyCipher.doFinal(decodedCipherText));
    }
}
