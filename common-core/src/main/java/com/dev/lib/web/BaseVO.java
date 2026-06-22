package com.dev.lib.web;

import com.alibaba.fastjson2.annotation.JSONField;
import com.dev.lib.web.serialize.FieldLoader;
import com.dev.lib.web.serialize.PopulateField;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public abstract class BaseVO {

    @JSONField(name = "id")
    private String bizId;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @JSONField(name = "creator")
    @PopulateField(loader = FieldLoader.USER_LOADER)
    private Long creatorId;

    @JSONField(name = "modifier")
    @PopulateField(loader = FieldLoader.USER_LOADER)
    private Long modifierId;

}
