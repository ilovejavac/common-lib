package com.dev.lib.web.dict.model.dto;

import com.dev.lib.entity.EntityStatus;
import com.dev.lib.entity.dsl.Condition;
import com.dev.lib.entity.dsl.QueryType;
import com.dev.lib.web.dict.data.DictType;
import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;

public class DictTypeDTO {
    private DictTypeDTO() {
    }

    @Data
    @AutoMapper(target = DictType.class, reverseConvertGenerate = false)
    public static class CreateType {
        private String typeCode;
        private String typeName;
        private Integer sort;
        private EntityStatus status;
    }

    @Data
    @AutoMapper(target = DictType.class, reverseConvertGenerate = false)
    public static class UpdateType extends CreateType {
        @JsonProperty("id")
        @JsonAlias("id")
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