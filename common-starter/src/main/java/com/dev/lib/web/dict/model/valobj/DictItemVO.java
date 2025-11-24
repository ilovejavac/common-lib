package com.dev.lib.web.dict.model.valobj;

import com.dev.lib.entity.EntityStatus;
import com.dev.lib.web.dict.data.DictItemEntity;
import com.dev.lib.web.model.BaseVO;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;

@Data
@AutoMapper(target = DictItemEntity.class)
public class DictItemVO extends BaseVO {
    private String itemCode;

    private String itemLabel;

    private String css;

    private Integer sort;

    private EntityStatus status;
}
