package com.dev.lib.web.dict.model.valobj;

import com.dev.lib.entity.EntityStatus;
import lombok.Data;

@Data
public class DictItemVO {
    private String bizId;

    private String itemCode;

    private String itemLabel;

    private String css;

    private Integer sort;

    private EntityStatus status;
}
