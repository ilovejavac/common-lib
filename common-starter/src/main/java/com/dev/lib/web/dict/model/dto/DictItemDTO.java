package com.dev.lib.web.dict.model.dto;

import com.dev.lib.entity.EntityStatus;
import com.dev.lib.entity.dsl.Condition;
import com.dev.lib.entity.dsl.QueryType;
import com.dev.lib.web.dict.data.DictItemEntity;
import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;

public class DictItemDTO {
    private DictItemDTO() {
    }

    @Data
    @AutoMapper(target = DictItemEntity.class, reverseConvertGenerate = false)
    public static class CreateItem {
        private String itemCode;
        private String itemLabel;
        private String css;
        private Integer sort;
        private EntityStatus status;
    }

    @Data
    @AutoMapper(target = DictItemEntity.class, reverseConvertGenerate = false)
    public static class UpdateItem extends CreateItem {
        @JsonAlias("id")
        @JsonProperty("id")
        private String bizId;
    }

    @Data
    public static class Query {
        @Condition(type = QueryType.LIKE, field = "itemCode")
        private String itemCodeLike;
        @Condition(type = QueryType.LIKE, field = "itemLabel")
        private String itemLabelLike;
        @Condition(type = QueryType.LIKE, field = "css")
        private String cssLike;

        @Condition(type = QueryType.EQ)
        private EntityStatus status;
    }
}