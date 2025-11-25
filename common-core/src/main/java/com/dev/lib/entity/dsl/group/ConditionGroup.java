package com.dev.lib.entity.dsl.group;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class ConditionGroup {
    private LogicalOperator operator = LogicalOperator.AND;
    private List<ConditionItem> conditions = new ArrayList<>();
    private List<ConditionGroup> groups = new ArrayList<>();  // 嵌套组
}