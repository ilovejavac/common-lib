package com.dev.lib.aksk.domain.model;

import lombok.Getter;
import lombok.Setter;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.Function;

@Getter
@Setter
public class AkskVerificationRequest {

    private String method;

    private String path;

    private byte[] body = new byte[0];

    private String clientIp;

    private AkskHeaders headers;

    private Function<String, String> headerLookup;

    private Set<String> requiredScopes = new LinkedHashSet<>();

    public String header(String name) {

        if (headerLookup == null) {
            return null;
        }
        return headerLookup.apply(name);
    }
}
