package com.dev.lib.security.aksk;

import com.dev.lib.aksk.domain.model.AkskAuthentication;
import com.dev.lib.security.util.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Component
public class AkskSecurityContextAdapter {

    public static final String AKSK_ROLE = "AKSK";

    public UserDetails toUserDetails(AkskAuthentication authentication) {

        if (authentication == null) {
            throw new IllegalArgumentException("AK/SK authentication must not be null");
        }

        return UserDetails.builder()
                .id(syntheticUserId(authentication))
                .username(username(authentication))
                .realName(authentication.getSubjectName())
                .roles(List.of(AKSK_ROLE))
                .permissions(new ArrayList<>(Optional.ofNullable(authentication.getScopes()).orElse(Set.of())))
                .validated(true)
                .clientIp(authentication.getClientIp())
                .extra(extra(authentication))
                .build();
    }

    private Long syntheticUserId(AkskAuthentication authentication) {

        int hash = authentication.getCredentialId() == null
                   ? Optional.ofNullable(authentication.getAccessKey()).orElse("aksk").hashCode()
                   : authentication.getCredentialId().hashCode();
        long unsignedHash = Integer.toUnsignedLong(hash);
        return -Math.max(1L, unsignedHash);
    }

    private String username(AkskAuthentication authentication) {

        if (StringUtils.hasText(authentication.getSubjectCode())) {
            return authentication.getSubjectCode();
        }
        if (StringUtils.hasText(authentication.getAccessKey())) {
            return authentication.getAccessKey();
        }
        return "aksk";
    }

    private Map<String, Object> extra(AkskAuthentication authentication) {

        Map<String, Object> extra = new LinkedHashMap<>();
        extra.put("akskCredentialId", authentication.getCredentialId());
        extra.put("accessKey", authentication.getAccessKey());
        extra.put("subjectCode", authentication.getSubjectCode());
        extra.put("properties", authentication.getProperties());
        return extra;
    }
}
