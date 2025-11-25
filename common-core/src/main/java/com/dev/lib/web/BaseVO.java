package com.dev.lib.web;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public abstract class BaseVO {
    @JsonProperty("id")
    private String bizId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    @JsonProperty("creator")
    private Long creatorId;

    @JsonProperty("modifier")
    private Long modifierId;
}
