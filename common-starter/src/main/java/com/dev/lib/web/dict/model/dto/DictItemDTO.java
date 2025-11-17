package com.dev.lib.web.dict.model.dto;

import com.dev.lib.entity.EntityStatus;
import com.dev.lib.web.dict.pojo.DictItemEntity;
import io.github.linpeilie.annotations.AutoMapper;
import io.github.linpeilie.annotations.AutoMapping;
import lombok.Data;

@Data
@AutoMapper(target = DictItemEntity.class)
public class DictItemDTO {
    private String itemCode;
    private String itemLabel;
    private String css;
    @AutoMapping(defaultValue = "0")
    private Integer sort;
    @AutoMapping(defaultValue = "ENABLE")
    private EntityStatus status;
    private Long typeId;
}