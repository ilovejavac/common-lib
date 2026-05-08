package com.dev.lib.aksk.domain.service;

import com.dev.lib.aksk.config.AkskProperties;
import com.dev.lib.aksk.data.AkskCredential;
import com.dev.lib.aksk.domain.model.AkskAuthentication;
import com.dev.lib.aksk.domain.model.AkskHeaders;
import com.dev.lib.aksk.domain.model.AkskStatus;
import com.dev.lib.aksk.domain.model.AkskVerificationRequest;
import com.dev.lib.exceptions.BizException;
import com.dev.lib.web.model.StandardErrorCodes;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Service
public class AkskVerifier {

    private static final Duration DEFAULT_TIMESTAMP_TOLERANCE = Duration.ofMinutes(5);

    private final AkskService service;

    private final AkskSigner signer;

    private final AkskProperties properties;

    private final Clock clock;

    @Autowired
    public AkskVerifier(AkskService service, AkskSigner signer, AkskProperties properties) {

        this(service, signer, properties, Clock.systemUTC());
    }

    AkskVerifier(AkskService service, AkskSigner signer, AkskProperties properties, Clock clock) {

        this.service = service;
        this.signer = signer;
        this.properties = properties == null ? new AkskProperties() : properties;
        this.clock = clock == null ? Clock.systemUTC() : clock;
    }

    Clock clock() {

        return clock;
    }

    public AkskAuthentication verify(AkskVerificationRequest request) {

        if (request == null) {
            throw new BizException(StandardErrorCodes.PARAM_INVALID, "AK/SK 验证请求不能为空");
        }

        AkskHeaders headers = request.getHeaders();
        if (headers == null) {
            throw new BizException(StandardErrorCodes.PARAM_INVALID, "AK/SK 请求头配置不能为空");
        }

        String accessKey = requiredHeader(request, headers.accessKeyHeader());
        String timestamp = requiredHeader(request, headers.timestampHeader());
        String signature = requiredHeader(request, headers.signatureHeader());
        long timestampMillis = parseTimestamp(timestamp);
        validateTimestamp(timestampMillis);

        AkskCredential.Entity credential = service.findActiveByAccessKey(accessKey)
                .orElseThrow(() -> authenticationFailed("AK/SK 凭证无效"));
        validateCredential(credential);
        validateSignature(request, timestamp, signature, credential);
        validateScopes(request.getRequiredScopes(), credential.getScopes());

        LocalDateTime authenticatedAt = LocalDateTime.now(clock);
        service.touchLastUsed(credential.getBizId(), request.getClientIp(), authenticatedAt);
        return authentication(credential, request.getClientIp(), authenticatedAt);
    }

    private String requiredHeader(AkskVerificationRequest request, String headerName) {

        String value = request.header(headerName);
        if (!StringUtils.hasText(value)) {
            throw new BizException(StandardErrorCodes.HEADER_MISSING, "缺少 AK/SK 请求头: " + headerName);
        }
        return value;
    }

    private long parseTimestamp(String timestamp) {

        try {
            return Long.parseLong(timestamp);
        } catch (NumberFormatException ex) {
            throw new BizException(StandardErrorCodes.PARAM_INVALID, "AK/SK 时间戳格式无效");
        }
    }

    private void validateTimestamp(long timestampMillis) {

        long drift = Math.abs(clock.millis() - timestampMillis);
        Duration tolerance = Optional.ofNullable(properties.getTimestampTolerance())
                .orElse(DEFAULT_TIMESTAMP_TOLERANCE);
        if (drift > tolerance.toMillis()) {
            throw authenticationFailed("AK/SK 时间戳已过期");
        }
    }

    private void validateCredential(AkskCredential.Entity credential) {

        if (credential.getStatus() != AkskStatus.active) {
            throw authenticationFailed("AK/SK 凭证已禁用");
        }
        LocalDateTime expireAt = credential.getExpireAt();
        if (expireAt != null && expireAt.isBefore(LocalDateTime.now(clock))) {
            throw authenticationFailed("AK/SK 凭证已过期");
        }
    }

    private void validateSignature(
            AkskVerificationRequest request,
            String timestamp,
            String actualSignature,
            AkskCredential.Entity credential
    ) {

        byte[] body = request.getBody() == null ? new byte[0] : request.getBody();
        String bodySha256 = signer.sha256Hex(body);
        String signingText = signer.buildSigningText(
                request.getMethod(),
                request.getPath(),
                timestamp,
                bodySha256
        );
        String expectedSignature = signer.hmacSha256Hex(credential.getSecretKey(), signingText);
        if (!signer.matches(expectedSignature, actualSignature)) {
            throw authenticationFailed("AK/SK 签名无效");
        }
    }

    private void validateScopes(Set<String> requiredScopes, Set<String> credentialScopes) {

        if (requiredScopes == null || requiredScopes.isEmpty()) {
            return;
        }
        Set<String> grantedScopes = credentialScopes == null ? Set.of() : credentialScopes;
        boolean matched = requiredScopes.stream().anyMatch(grantedScopes::contains);
        if (!matched) {
            throw new BizException(StandardErrorCodes.PERMISSION_DENIED, "AK/SK 授权范围不足");
        }
    }

    private AkskAuthentication authentication(
            AkskCredential.Entity credential,
            String clientIp,
            LocalDateTime authenticatedAt
    ) {

        AkskAuthentication authentication = new AkskAuthentication();
        authentication.setCredentialId(credential.getBizId());
        authentication.setAccessKey(credential.getAccessKey());
        authentication.setSubjectName(credential.getSubjectName());
        authentication.setSubjectCode(credential.getSubjectCode());
        authentication.setScopes(new LinkedHashSet<>(Optional.ofNullable(credential.getScopes()).orElse(Set.of())));
        authentication.setProperties(new LinkedHashMap<>(Optional.ofNullable(credential.getProperties()).orElse(Map.of())));
        authentication.setClientIp(clientIp);
        authentication.setAuthenticatedAt(authenticatedAt);
        return authentication;
    }

    private BizException authenticationFailed(String message) {

        return new BizException(StandardErrorCodes.AUTHENTICATION_FAILED, message);
    }
}
