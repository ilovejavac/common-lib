package com.dev.lib.jpa.entity;

import com.dev.lib.jpa.TransactionHelper;
import org.hibernate.Session;

import java.sql.SQLException;
import java.sql.Statement;
import java.util.Objects;
import java.util.regex.Pattern;

public class DropTableBuilder<T extends JpaEntity> {

    private static final Pattern TABLE_NAME_PATTERN = Pattern.compile(
            "([A-Za-z_][A-Za-z0-9_]*|`[A-Za-z_][A-Za-z0-9_]*`)(\\.([A-Za-z_][A-Za-z0-9_]*|`[A-Za-z_][A-Za-z0-9_]*`)){0,2}"
    );

    private final BaseRepositoryImpl<T> impl;

    private final String tableName;

    private boolean physical;

    public DropTableBuilder(BaseRepositoryImpl<T> impl, String tableName) {

        this.impl = Objects.requireNonNull(impl, "Repository 实现不能为空");
        this.tableName = tableName;
    }

    public DropTableBuilder<T> physical(boolean physical) {

        this.physical = physical;
        return this;
    }

    public EtlSqlStatementResult execute() {

        String validatedTableName = validateTableName(tableName);
        String backupTableName = backupTableName(validatedTableName);
        String renameSql = "ALTER TABLE " + validatedTableName + " RENAME TO " + backupLeafName(validatedTableName);
        String dropSql = "DROP TABLE IF EXISTS " + backupTableName;
        return TransactionHelper.callWithEntityManagerFactory(impl.getEntityManagerFactory(), () ->
                impl.getEntityManager()
                        .unwrap(Session.class)
                        .doReturningWork(connection -> {
                            try (Statement statement = connection.createStatement()) {
                                statement.execute(renameSql);
                                if (!physical) {
                                    return new EtlSqlStatementResult(1, "RENAME_TABLE", false, statement.getUpdateCount(), renameSql);
                                }

                                statement.execute(dropSql);
                                return new EtlSqlStatementResult(2, "DROP_TABLE", false, statement.getUpdateCount(), dropSql);
                            } catch (SQLException e) {
                                throw new SQLException("DROP TABLE 执行失败: " + validatedTableName, e);
                            }
                        })
        );
    }

    private static String validateTableName(String tableName) {

        if (tableName == null || tableName.isBlank()) {
            throw new IllegalArgumentException("只允许传入单张表名");
        }

        String trimmed = tableName.trim();
        if (!TABLE_NAME_PATTERN.matcher(trimmed).matches()) {
            throw new IllegalArgumentException("只允许传入单张表名");
        }
        return trimmed;
    }

    private static String backupTableName(String tableName) {

        int lastDot = tableName.lastIndexOf('.');
        if (lastDot < 0) {
            return backupLeafName(tableName);
        }
        return tableName.substring(0, lastDot + 1) + backupLeafName(tableName);
    }

    private static String backupLeafName(String tableName) {

        int lastDot = tableName.lastIndexOf('.');
        String leaf = lastDot < 0 ? tableName : tableName.substring(lastDot + 1);
        boolean quoted = leaf.startsWith("`") && leaf.endsWith("`");
        String unquoted = quoted ? leaf.substring(1, leaf.length() - 1) : leaf;
        String backup = "back_" + unquoted;
        return quoted ? "`" + backup + "`" : backup;
    }
}
