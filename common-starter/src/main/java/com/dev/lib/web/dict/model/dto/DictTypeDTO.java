package com.dev.lib.web.dict.model.dto;

import com.dev.lib.entity.EntityStatus;
import lombok.Data;

@Data
public class DictTypeDTO {
    private Long id;
    private String typeCode;
    private String typeName;
    private Integer sort;
    private EntityStatus status;
}