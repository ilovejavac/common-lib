package com.dev.lib.web.dict.model.valobj;

import com.dev.lib.entity.EntityStatus;
import com.dev.lib.web.dict.data.DictType;
import com.dev.lib.web.model.BaseVO;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;

@Data
@AutoMapper(target = DictType.class)
public class DictTypeVO extends BaseVO {
    private String typeCode;

    private String typeName;

    private Integer sort;

    private EntityStatus status;
}
