package com.dev.lib.dict.domain.model.valobj;

import com.dev.lib.entity.EntityStatus;
import com.dev.lib.web.BaseVO;
import lombok.Data;

@Data
public class DictItemVO extends BaseVO {

    private String itemCode;

    private String itemLabel;

    private String css;

    private Integer sort;

    private EntityStatus status;

}
