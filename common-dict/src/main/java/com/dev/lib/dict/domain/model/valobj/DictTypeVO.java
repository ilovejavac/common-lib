package com.dev.lib.dict.domain.model.valobj;

import com.dev.lib.entity.EntityStatus;
import com.dev.lib.web.BaseVO;
import lombok.Data;

@Data
public class DictTypeVO extends BaseVO {

    private String typeCode;

    private String typeName;

    private Integer sort;

    private EntityStatus status;

}
