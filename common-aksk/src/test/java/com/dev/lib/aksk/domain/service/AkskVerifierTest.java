package com.dev.lib.aksk.domain.service;

import com.dev.lib.aksk.config.AkskProperties;
import com.dev.lib.aksk.data.AkskCredential;
import com.dev.lib.aksk.domain.model.AkskAuthentication;
import com.dev.lib.aksk.domain.model.AkskHeaders;
import com.dev.lib.aksk.domain.model.AkskStatus;
import com.dev.lib.aksk.domain.model.AkskVerificationRequest;
import com.dev.lib.exceptions.BizException;
import com.dev.lib.web.model.StandardErrorCodes;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AkskVerifierTest {

    private static final long NOW_MILLIS = 1714372800000L;

    private static final String ACCESS_KEY = "ak_valid";

    private static final String SECRET_KEY = "sk_secret";

    private static final AkskHeaders HEADERS = new AkskHeaders(
            "X-Aksk-Access-Key",
            "X-Aksk-Timestamp",
            "X-Aksk-Signature"
    );

    @Mock
    private AkskService service;

    private AkskSigner signer;

    private AkskVerifier verifier;

    @BeforeEach
    void setUp() {

        signer = new AkskSigner();
        verifier = new AkskVerifier(
                service,
                signer,
                new AkskProperties(),
                Clock.fixed(Instant.ofEpochMilli(NOW_MILLIS), ZoneId.systemDefault())
        );
    }

    @Test
    void verifyShouldReturnAuthenticationForValidCredentialAndSignature() {

        AkskCredential.Entity credential = credential(AkskStatus.active, Set.of("scope:a"), null);
        when(service.findActiveByAccessKey(ACCESS_KEY)).thenReturn(Optional.of(credential));

        AkskAuthentication authentication = verifier.verify(validRequest(Set.of("scope:a")));

        assertThat(authentication.getCredentialId()).isEqualTo("cred-1");
        assertThat(authentication.getAccessKey()).isEqualTo(ACCESS_KEY);
        assertThat(authentication.getSubjectName()).isEqualTo("Partner");
        assertThat(authentication.getSubjectCode()).isEqualTo("partner");
        assertThat(authentication.getScopes()).containsExactly("scope:a");
        assertThat(authentication.getProperties()).containsEntry("tenant", "blue");
        assertThat(authentication.getClientIp()).isEqualTo("127.0.0.1");
        assertThat(authentication.getAuthenticatedAt()).isEqualTo(LocalDateTime.now(verifier.clock()));
        verify(service).touchLastUsed(eq("cred-1"), eq("127.0.0.1"), any(LocalDateTime.class));
    }

    @Test
    void missingAccessKeyShouldFailWithHeaderMissing() {

        AkskVerificationRequest request = validRequest(Set.of("scope:a"));
        request.setHeaderLookup(headerLookup(Map.of(
                HEADERS.timestampHeader(), String.valueOf(NOW_MILLIS),
                HEADERS.signatureHeader(), "sig"
        )));

        assertBizCode(request, StandardErrorCodes.HEADER_MISSING);
    }

    @Test
    void missingTimestampShouldFailWithHeaderMissing() {

        AkskVerificationRequest request = validRequest(Set.of("scope:a"));
        request.setHeaderLookup(headerLookup(Map.of(
                HEADERS.accessKeyHeader(), ACCESS_KEY,
                HEADERS.signatureHeader(), "sig"
        )));

        assertBizCode(request, StandardErrorCodes.HEADER_MISSING);
    }

    @Test
    void missingSignatureShouldFailWithHeaderMissing() {

        AkskVerificationRequest request = validRequest(Set.of("scope:a"));
        request.setHeaderLookup(headerLookup(Map.of(
                HEADERS.accessKeyHeader(), ACCESS_KEY,
                HEADERS.timestampHeader(), String.valueOf(NOW_MILLIS)
        )));

        assertBizCode(request, StandardErrorCodes.HEADER_MISSING);
    }

    @Test
    void invalidTimestampShouldFailWithParamInvalid() {

        AkskVerificationRequest request = validRequest(Set.of("scope:a"));
        request.setHeaderLookup(headerLookup(signedHeaders("not-a-number", "{}".getBytes(StandardCharsets.UTF_8))));

        assertBizCode(request, StandardErrorCodes.PARAM_INVALID);
    }

    @Test
    void timestampOutsideToleranceShouldFailWithAuthenticationFailed() {

        long oldTimestamp = NOW_MILLIS - 301_000L;
        AkskVerificationRequest request = validRequest(Set.of("scope:a"));
        request.setHeaderLookup(headerLookup(signedHeaders(String.valueOf(oldTimestamp), request.getBody())));

        assertBizCode(request, StandardErrorCodes.AUTHENTICATION_FAILED);
    }

    @Test
    void missingAccessKeyRecordShouldFailWithAuthenticationFailed() {

        when(service.findActiveByAccessKey(ACCESS_KEY)).thenReturn(Optional.empty());

        assertBizCode(validRequest(Set.of("scope:a")), StandardErrorCodes.AUTHENTICATION_FAILED);
    }

    @Test
    void disabledCredentialShouldFailWithAuthenticationFailed() {

        when(service.findActiveByAccessKey(ACCESS_KEY))
                .thenReturn(Optional.of(credential(AkskStatus.disable, Set.of("scope:a"), null)));

        assertBizCode(validRequest(Set.of("scope:a")), StandardErrorCodes.AUTHENTICATION_FAILED);
    }

    @Test
    void expiredCredentialShouldFailWithAuthenticationFailed() {

        when(service.findActiveByAccessKey(ACCESS_KEY)).thenReturn(Optional.of(credential(
                AkskStatus.active,
                Set.of("scope:a"),
                LocalDateTime.now(verifier.clock()).minusSeconds(1)
        )));

        assertBizCode(validRequest(Set.of("scope:a")), StandardErrorCodes.AUTHENTICATION_FAILED);
    }

    @Test
    void wrongSignatureShouldFailWithAuthenticationFailed() {

        when(service.findActiveByAccessKey(ACCESS_KEY))
                .thenReturn(Optional.of(credential(AkskStatus.active, Set.of("scope:a"), null)));
        AkskVerificationRequest request = validRequest(Set.of("scope:a"));
        request.setHeaderLookup(headerLookup(Map.of(
                HEADERS.accessKeyHeader(), ACCESS_KEY,
                HEADERS.timestampHeader(), String.valueOf(NOW_MILLIS),
                HEADERS.signatureHeader(), "bad-signature"
        )));

        assertBizCode(request, StandardErrorCodes.AUTHENTICATION_FAILED);
    }

    @Test
    void emptyRequiredScopesShouldOnlyVerifyIdentity() {

        when(service.findActiveByAccessKey(ACCESS_KEY))
                .thenReturn(Optional.of(credential(AkskStatus.active, Set.of(), null)));

        AkskAuthentication authentication = verifier.verify(validRequest(Set.of()));

        assertThat(authentication.getCredentialId()).isEqualTo("cred-1");
    }

    @Test
    void credentialScopesShouldUseAnyMatch() {

        when(service.findActiveByAccessKey(ACCESS_KEY))
                .thenReturn(Optional.of(credential(AkskStatus.active, Set.of("scope:a", "scope:b"), null)));

        AkskAuthentication authentication = verifier.verify(validRequest(Set.of("scope:x", "scope:b")));

        assertThat(authentication.getCredentialId()).isEqualTo("cred-1");
    }

    @Test
    void scopeMismatchShouldFailWithPermissionDenied() {

        when(service.findActiveByAccessKey(ACCESS_KEY))
                .thenReturn(Optional.of(credential(AkskStatus.active, Set.of("scope:a"), null)));

        assertBizCode(validRequest(Set.of("scope:x")), StandardErrorCodes.PERMISSION_DENIED);
    }

    private void assertBizCode(AkskVerificationRequest request, int code) {

        assertThatThrownBy(() -> verifier.verify(request))
                .isInstanceOfSatisfying(BizException.class, ex -> assertThat(ex.getCoder()).isEqualTo(code));
    }

    private AkskVerificationRequest validRequest(Set<String> requiredScopes) {

        byte[] body = "{}".getBytes(StandardCharsets.UTF_8);
        AkskVerificationRequest request = new AkskVerificationRequest();
        request.setMethod("POST");
        request.setPath("/datalake/api/ads/datares/catalog_daily/query");
        request.setBody(body);
        request.setClientIp("127.0.0.1");
        request.setHeaders(HEADERS);
        request.setRequiredScopes(new LinkedHashSet<>(requiredScopes));
        request.setHeaderLookup(headerLookup(signedHeaders(String.valueOf(NOW_MILLIS), body)));
        return request;
    }

    private Map<String, String> signedHeaders(String timestamp, byte[] body) {

        String bodySha256 = signer.sha256Hex(body);
        String signingText = signer.buildSigningText(
                "POST",
                "/datalake/api/ads/datares/catalog_daily/query",
                timestamp,
                bodySha256
        );
        String signature = signer.hmacSha256Hex(SECRET_KEY, signingText);

        Map<String, String> headers = new LinkedHashMap<>();
        headers.put(HEADERS.accessKeyHeader(), ACCESS_KEY);
        headers.put(HEADERS.timestampHeader(), timestamp);
        headers.put(HEADERS.signatureHeader(), signature);
        return headers;
    }

    private java.util.function.Function<String, String> headerLookup(Map<String, String> headers) {

        return headers::get;
    }

    private AkskCredential.Entity credential(AkskStatus status, Set<String> scopes, LocalDateTime expireAt) {

        AkskCredential.Entity entity = new AkskCredential.Entity();
        entity.setBizId("cred-1");
        entity.setAccessKey(ACCESS_KEY);
        entity.setSecretKey(SECRET_KEY);
        entity.setSubjectName("Partner");
        entity.setSubjectCode("partner");
        entity.setScopes(new LinkedHashSet<>(scopes));
        entity.setProperties(new LinkedHashMap<>(Map.of("tenant", "blue")));
        entity.setStatus(status);
        entity.setExpireAt(expireAt);
        return entity;
    }
}
