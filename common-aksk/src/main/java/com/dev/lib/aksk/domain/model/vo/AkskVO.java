package com.dev.lib.aksk.domain.model.vo;

import com.dev.lib.aksk.domain.model.AkskStatus;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;

public class AkskVO {

    private AkskVO() {
    }

    @Getter
    @Setter
    public static class CreateResult {

        private String id;

        private String accessKey;

        private String secretKey;
    }

    @Getter
    @Setter
    public static class ResetSecretResult {

        private String id;

        private String accessKey;

        private String secretKey;
    }

    @Getter
    @Setter
    public static class ListItem {

        private String id;

        private String accessKey;

        private String subjectName;

        private String subjectCode;

        private Set<String> scopes;

        private AkskStatus status;

        private LocalDateTime expireAt;

        private LocalDateTime lastUsedAt;

        private String lastUsedIp;
    }

    @Getter
    @Setter
    public static class Detail {

        private String id;

        private String accessKey;

        private String subjectName;

        private String subjectCode;

        private String contactName;

        private String contactPhone;

        private String description;

        private Set<String> scopes;

        private Map<String, String> properties;

        private AkskStatus status;

        private LocalDateTime expireAt;

        private LocalDateTime lastUsedAt;

        private String lastUsedIp;
    }
}
