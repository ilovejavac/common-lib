package com.dev.lib.web.dict.model.dto;

import com.dev.lib.entity.EntityStatus;
import com.dev.lib.web.dict.pojo.DictType;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;

@Data
@AutoMapper(target = DictType.class)
public class DictTypeDTO {
    private String bizId;

    private String typeCode;
    private String typeName;
    private Integer sort;
    private EntityStatus status;
}