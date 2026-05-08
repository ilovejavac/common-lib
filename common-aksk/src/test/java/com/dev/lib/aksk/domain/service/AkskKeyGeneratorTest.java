package com.dev.lib.aksk.domain.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AkskKeyGeneratorTest {

    private final AkskKeyGenerator generator = new AkskKeyGenerator();

    @Test
    void generateAccessKeyShouldUseAkPrefixAndEnoughRandomCharacters() {

        String accessKey = generator.generateAccessKey();

        assertThat(accessKey)
                .startsWith("ak_")
                .hasSizeGreaterThanOrEqualTo(35);
    }

    @Test
    void generateSecretKeyShouldUseSkPrefixAndEnoughRandomCharacters() {

        String secretKey = generator.generateSecretKey();

        assertThat(secretKey)
                .startsWith("sk_")
                .hasSizeGreaterThanOrEqualTo(51);
    }

    @Test
    void generatedKeysShouldNotBeBlankOrRepeated() {

        String firstAccessKey = generator.generateAccessKey();
        String secondAccessKey = generator.generateAccessKey();
        String firstSecretKey = generator.generateSecretKey();
        String secondSecretKey = generator.generateSecretKey();

        assertThat(firstAccessKey).isNotBlank();
        assertThat(firstSecretKey).isNotBlank();
        assertThat(firstAccessKey).isNotEqualTo(secondAccessKey);
        assertThat(firstSecretKey).isNotEqualTo(secondSecretKey);
    }
}
