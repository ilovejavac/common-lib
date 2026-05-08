package com.dev.lib.aksk.domain.model;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

@Getter
@Setter
public class AkskAuthentication {

    private String credentialId;

    private String accessKey;

    private String subjectName;

    private String subjectCode;

    private Set<String> scopes = new LinkedHashSet<>();

    private Map<String, String> properties = new LinkedHashMap<>();

    private String clientIp;

    private LocalDateTime authenticatedAt;
}
