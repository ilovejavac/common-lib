package com.dev.lib.aksk.domain.model.dto;

import com.dev.lib.aksk.domain.model.AkskStatus;
import com.dev.lib.entity.dsl.Condition;
import com.dev.lib.entity.dsl.QueryType;
import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;

public class AkskDTO {

    private AkskDTO() {
    }

    @Getter
    @Setter
    public static class Create {

        @NotBlank(message = "主体名称不能为空")
        private String subjectName;

        private String subjectCode;

        private String contactName;

        private String contactPhone;

        private String description;

        @Size(max = 64, message = "授权范围不能超过 64 个")
        private Set<String> scopes;

        @Size(max = 64, message = "扩展属性不能超过 64 个")
        private Map<String, String> properties;

        private LocalDateTime expireAt;
    }

    @Getter
    @Setter
    public static class Update {

        @NotBlank(message = "凭证 ID 不能为空")
        @JsonProperty("id")
        @JsonAlias("id")
        private String bizId;

        private String subjectName;

        private String subjectCode;

        private String contactName;

        private String contactPhone;

        private String description;

        @Size(max = 64, message = "授权范围不能超过 64 个")
        private Set<String> scopes;

        @Size(max = 64, message = "扩展属性不能超过 64 个")
        private Map<String, String> properties;

        private LocalDateTime expireAt;
    }

    @Getter
    @Setter
    public static class Query {

        @Condition(type = QueryType.LIKE, field = "accessKey")
        private String accessKeyLike;

        @Condition(type = QueryType.LIKE, field = "subjectName")
        private String subjectNameLike;

        private AkskStatus status;
    }
}
