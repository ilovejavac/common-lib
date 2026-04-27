package com.dev.lib.jpa.entity;

public record EtlSqlStatementResult(
        int index,
        String statementType,
        boolean resultSet,
        int updateCount,
        String statementSummary
) {
}
