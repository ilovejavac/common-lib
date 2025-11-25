package com.dev.lib.entity.dsl.group;

import com.dev.lib.entity.dsl.QueryType;
import lombok.Data;

@Data
public class ConditionItem {
    private String field;
    private QueryType type;
    private Object value;
}