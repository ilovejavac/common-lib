package com.dev.lib.entity.dsl;

/**
 * 查询类型枚举
 */
public enum QueryType {
    // 普通条件
    EQ,           // =
    NE,           // !=
    GT,           // >
    GE,           // >=
    LT,           // <
    LE,           // <=
    LIKE,         // LIKE %value%
    START_WITH,   // LIKE value%
    END_WITH,     // LIKE %value
    IN,           // IN (...)
    NOT_IN,       // NOT IN (...)
    IS_NULL,      // IS NULL
    IS_NOT_NULL,  // IS NOT NULL
    BETWEEN,      // BETWEEN ... AND ...

    // 子查询条件
    EXISTS,       // EXISTS (SELECT ...)
    NOT_EXISTS,   // NOT EXISTS (SELECT ...)

    // 占位
    EMPTY
}