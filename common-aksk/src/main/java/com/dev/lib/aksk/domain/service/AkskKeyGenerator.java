package com.dev.lib.aksk.domain.service;

import java.security.SecureRandom;
import java.util.Base64;

public class AkskKeyGenerator {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private static final Base64.Encoder ENCODER = Base64.getUrlEncoder().withoutPadding();

    public String generateAccessKey() {

        return "ak_" + randomUrlSafe(24);
    }

    public String generateSecretKey() {

        return "sk_" + randomUrlSafe(36);
    }

    private String randomUrlSafe(int byteLength) {

        byte[] bytes = new byte[byteLength];
        SECURE_RANDOM.nextBytes(bytes);
        return ENCODER.encodeToString(bytes);
    }
}
