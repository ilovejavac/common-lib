package com.dev.lib.web.dict.model.dto;

import com.dev.lib.entity.dsl.Condition;
import com.dev.lib.entity.dsl.DslQuery;
import com.dev.lib.entity.dsl.QueryType;
import com.dev.lib.web.dict.pojo.DictType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;

public class DictTypeRequest {
    private DictTypeRequest() {}

    @Data
    @AllArgsConstructor
    @EqualsAndHashCode(callSuper = true)
    public static class GetType extends DslQuery<DictType> {
        @Condition(type = QueryType.EQ, field = "bizId")
        private String id;
    }
}
