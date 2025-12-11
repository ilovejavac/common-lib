package com.dev.lib.web;

import com.dev.lib.web.serialize.FieldLoader;
import com.dev.lib.web.serialize.PopulateField;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public abstract class BaseVO {

    @JsonProperty("id")
    private String        bizId;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @JsonProperty("creator")
    @PopulateField(loader = FieldLoader.USER_LOADER)
    private Long creatorId;

    @JsonProperty("modifier")
    @PopulateField(loader = FieldLoader.USER_LOADER)
    private Long modifierId;

}
