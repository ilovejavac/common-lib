package com.dev.lib.aksk.domain.service;

import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public class AkskSigner {

    private static final String HMAC_SHA256 = "HmacSHA256";

    public String sha256Hex(byte[] body) {

        try {
            byte[] source = body == null ? new byte[0] : body;
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(source));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 algorithm is not available", ex);
        }
    }

    public String buildSigningText(String method, String path, String timestamp, String bodySha256) {

        return String.join(
                "\n",
                method,
                path,
                timestamp,
                bodySha256
        );
    }

    public String hmacSha256Hex(String secretKey, String signingText) {

        try {
            Mac mac = Mac.getInstance(HMAC_SHA256);
            mac.init(new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), HMAC_SHA256));
            return HexFormat.of().formatHex(mac.doFinal(signingText.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("HmacSHA256 algorithm is not available", ex);
        } catch (InvalidKeyException ex) {
            throw new IllegalArgumentException("Invalid AK/SK secret key", ex);
        }
    }

    public boolean matches(String expectedSignature, String actualSignature) {

        if (expectedSignature == null || actualSignature == null) {
            return false;
        }

        return MessageDigest.isEqual(
                expectedSignature.getBytes(StandardCharsets.UTF_8),
                actualSignature.getBytes(StandardCharsets.UTF_8)
        );
    }
}
