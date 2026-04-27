package com.dev.lib.util.encrypt.impl;

import com.dev.lib.config.properties.AppSecurityProperties;
import com.dev.lib.entity.encrypt.EncryptVersion;
import com.dev.lib.util.encrypt.Encryptor;
import com.dev.lib.util.encrypt.condition.ConditionalOnEncryptionStrategy;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.OAEPParameterSpec;
import javax.crypto.spec.PSource;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.MGF1ParameterSpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

/**
 * RSA 加密
 */
@Component
@RequiredArgsConstructor
@ConditionalOnEncryptionStrategy(EncryptVersion.RSA)
public class RSAEncryptionStrategy implements Encryptor, InitializingBean {

    private final AppSecurityProperties securityProperties;

    private static final String KEY_ALGORITHM = "RSA";

    private static final String CIPHER_TRANSFORMATION = "RSA/ECB/OAEPPadding";

    private static final OAEPParameterSpec OAEP_SHA256_SPEC = new OAEPParameterSpec(
            "SHA-256",
            "MGF1",
            MGF1ParameterSpec.SHA256,
            PSource.PSpecified.DEFAULT
    );

    private PublicKey publicKey;

    private PrivateKey privateKey;

    @Override
    public void afterPropertiesSet() throws Exception {

        String publicKeyStr  = securityProperties.getRsa().getPublicKey();
        String privateKeyStr = securityProperties.getRsa().getPrivateKey();

        if (publicKeyStr == null || publicKeyStr.isBlank()
                || privateKeyStr == null || privateKeyStr.isBlank()) {
            throw new IllegalStateException(
                    "app.security.rsa.public-key and app.security.rsa.private-key must both be configured when RSA encryption is enabled"
            );
        }

        KeyFactory keyFactory = KeyFactory.getInstance(KEY_ALGORITHM);

        byte[]             publicKeyBytes = Base64.getDecoder().decode(publicKeyStr);
        X509EncodedKeySpec publicKeySpec  = new X509EncodedKeySpec(publicKeyBytes);
        this.publicKey = keyFactory.generatePublic(publicKeySpec);

        byte[]              privateKeyBytes = Base64.getDecoder().decode(privateKeyStr);
        PKCS8EncodedKeySpec privateKeySpec  = new PKCS8EncodedKeySpec(privateKeyBytes);
        this.privateKey = keyFactory.generatePrivate(privateKeySpec);
    }

    @Override
    public EncryptVersion getVersion() {

        return EncryptVersion.RSA;
    }

    @Override
    public String encrypt(String plainText) {

        try {
            Cipher cipher = Cipher.getInstance(CIPHER_TRANSFORMATION);
            cipher.init(
                    Cipher.ENCRYPT_MODE,
                    publicKey,
                    OAEP_SHA256_SPEC
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
            Cipher cipher = Cipher.getInstance(CIPHER_TRANSFORMATION);
            cipher.init(
                    Cipher.DECRYPT_MODE,
                    privateKey,
                    OAEP_SHA256_SPEC
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
