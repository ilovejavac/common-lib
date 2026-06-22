package com.dev.lib.dict.domain.model.dto;

import com.alibaba.fastjson2.annotation.JSONField;
import com.dev.lib.entity.EntityStatus;
import com.dev.lib.entity.dsl.Condition;
import com.dev.lib.entity.dsl.QueryType;
import lombok.Data;

public class DictTypeDTO {

    private DictTypeDTO() {

    }

    @Data
    public static class CreateType {

        private String typeCode;

        private String typeName;

        private Integer sort;

        private EntityStatus status;

    }

    @Data
    public static class UpdateType extends CreateType {

        @JSONField(name = "id")
        private String bizId;

    }

    @Data
    public static class Query {

        @Condition(type = QueryType.LIKE, field = "typeCode")
        private String typeCodeLike;

        @Condition(type = QueryType.LIKE, field = "typeName")
        private String typeNameLike;

        @Condition(type = QueryType.EQ)
        private EntityStatus status;

    }

}
