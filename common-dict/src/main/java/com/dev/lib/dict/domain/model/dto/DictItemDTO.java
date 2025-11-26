package com.dev.lib.dict.domain.model.dto;

import com.dev.lib.entity.EntityStatus;
import com.dev.lib.entity.dsl.Condition;
import com.dev.lib.entity.dsl.QueryType;
import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

public class DictItemDTO {
    private DictItemDTO() {
    }

    @Data
    public static class CreateItem {
        private String itemCode;
        private String itemLabel;
        private String css;
        private Integer sort;
        private EntityStatus status;
    }

    @Data
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