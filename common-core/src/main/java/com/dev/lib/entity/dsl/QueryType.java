package com.dev.lib.entity.dsl;

public enum QueryType {
    EQ,           // =
    NE,           // !=
    GT,           // >
    GE,           // >=
    LT,           // 
    LE,           // <=
    LIKE,         // LIKE %value%
    START_WITH,   // LIKE value%
    END_WITH,     // LIKE %value
    IN,           // IN
    NOT_IN,       // NOT IN
    IS_NULL,      // IS NULL
    IS_NOT_NULL,  // IS NOT NULL
    BETWEEN,       // BETWEEN

    EMPTY
}