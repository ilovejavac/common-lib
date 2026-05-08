package com.dev.lib.aksk.domain.service;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class AkskSignerTest {

    private final AkskSigner signer = new AkskSigner();

    @Test
    void sha256HexShouldHashRawBytesAsLowercaseHex() {

        String digest = signer.sha256Hex("abc".getBytes(StandardCharsets.UTF_8));

        assertThat(digest)
                .isEqualTo("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad")
                .isLowerCase();
    }

    @Test
    void sha256HexShouldTreatEmptyBodyAndEmptyJsonObjectAsDifferentBodies() {

        String emptyBodyDigest = signer.sha256Hex(new byte[0]);
        String emptyJsonObjectDigest = signer.sha256Hex("{}".getBytes(StandardCharsets.UTF_8));

        assertThat(emptyBodyDigest)
                .isEqualTo("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855");
        assertThat(emptyJsonObjectDigest)
                .isEqualTo("44136fa355b3678a1146ad16f7e8649e94fb4fc21fe77e8310c060f61caaff8a");
        assertThat(emptyBodyDigest).isNotEqualTo(emptyJsonObjectDigest);
    }

    @Test
    void buildSigningTextShouldUseMethodPathTimestampAndBodyDigestInOrder() {

        String bodyDigest = signer.sha256Hex("{}".getBytes(StandardCharsets.UTF_8));

        String signingText = signer.buildSigningText(
                "POST",
                "/datalake/api/ads/datares/catalog_daily/query",
                "1714372800000",
                bodyDigest
        );

        assertThat(signingText).isEqualTo("""
                POST
                /datalake/api/ads/datares/catalog_daily/query
                1714372800000
                44136fa355b3678a1146ad16f7e8649e94fb4fc21fe77e8310c060f61caaff8a""");
    }

    @Test
    void hmacSha256HexShouldReturnLowercaseHex() {

        String signature = signer.hmacSha256Hex(
                "key",
                "The quick brown fox jumps over the lazy dog"
        );

        assertThat(signature)
                .isEqualTo("f7bc83f430538424b13298e6aa6fb143ef4d59a14946175997479dbc2d1a3cd8")
                .isLowerCase();
    }

    @Test
    void matchesShouldCompareSignaturesWithoutLeakingEqualityByShortCircuit() {

        String expected = signer.hmacSha256Hex("key", "payload");
        String actual = signer.hmacSha256Hex("key", "payload");
        String wrong = signer.hmacSha256Hex("key", "different-payload");

        assertThat(signer.matches(expected, actual)).isTrue();
        assertThat(signer.matches(expected, wrong)).isFalse();
        assertThat(signer.matches(expected, expected.substring(1))).isFalse();
        assertThat(signer.matches(expected, null)).isFalse();
    }
}
