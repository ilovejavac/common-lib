package com.dev.lib.web.dict.model.dto;

import com.dev.lib.entity.EntityStatus;
import lombok.Data;

@Data
public class DictItemDTO {
    private Long id;
    private String itemCode;
    private String itemLabel;
    private String css;
    private Integer sort;
    private EntityStatus status;
    private Long typeId;
}