package com.dev.lib.web.dict.model.dto;

import com.dev.lib.entity.dsl.Condition;
import com.dev.lib.entity.dsl.QueryType;
import com.dev.lib.entity.dsl.DslQuery;
import com.dev.lib.web.dict.pojo.DictItemEntity;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

public class DictItemRequest {
    private DictItemRequest() {
    }

    @Data
    @EqualsAndHashCode(callSuper = true)
    @AllArgsConstructor
    public static class GetItem extends DslQuery<DictItemEntity> {
        @Condition(type = QueryType.EQ)
        private String itemCode;
    }

    @Data
    @EqualsAndHashCode(callSuper = true)
    @AllArgsConstructor
    public static class ListItem extends DslQuery<DictItemEntity> {
        @Condition(type = QueryType.IN, field = "itemCode")
        private List<String> itemCodes;
    }
}
