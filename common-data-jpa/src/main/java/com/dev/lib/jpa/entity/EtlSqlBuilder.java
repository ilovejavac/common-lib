package com.dev.lib.jpa.entity;

import com.dev.lib.jpa.TransactionHelper;
import org.hibernate.Session;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class EtlSqlBuilder<T extends JpaEntity> {

    private static final int STATEMENT_SUMMARY_LIMIT = 160;

    private final BaseRepositoryImpl<T> impl;

    private final String sqlScript;

    public EtlSqlBuilder(BaseRepositoryImpl<T> impl, String sqlScript) {

        this.impl = Objects.requireNonNull(impl, "Repository 实现不能为空");
        this.sqlScript = sqlScript;
    }

    public List<EtlSqlStatementResult> execute() {

        List<EtlSqlStatement> statements = SqlScriptSupport.parse(sqlScript);
        SqlScriptSupport.validate(statements);

        return TransactionHelper.callWithEntityManagerFactory(impl.getEntityManagerFactory(), () ->
                impl.getEntityManager()
                        .unwrap(Session.class)
                        .doReturningWork(connection -> {
                            List<EtlSqlStatementResult> results = new ArrayList<>(statements.size());
                            for (EtlSqlStatement statement : statements) {
                                results.add(executeStatement(connection, statement));
                            }
                            return results;
                        })
        );
    }

    private EtlSqlStatementResult executeStatement(Connection connection, EtlSqlStatement statement) throws SQLException {

        try (Statement jdbcStatement = connection.createStatement()) {
            boolean resultSet = jdbcStatement.execute(statement.sql());
            return new EtlSqlStatementResult(
                    statement.index(),
                    statement.type(),
                    resultSet,
                    jdbcStatement.getUpdateCount(),
                    summary(statement.sql())
            );
        } catch (SQLException e) {
            throw new SQLException("ETL SQL 第 " + statement.index() + " 条执行失败: " + summary(statement.sql()), e);
        }
    }

    private static String summary(String sql) {

        String compact = sql.replaceAll("\\s+", " ").trim();
        if (compact.length() <= STATEMENT_SUMMARY_LIMIT) {
            return compact;
        }
        return compact.substring(0, STATEMENT_SUMMARY_LIMIT) + "...";
    }

    private record EtlSqlStatement(int index, String sql, String normalized, String type) {
    }

    private static final class SqlScriptSupport {

        private SqlScriptSupport() {
        }

        static List<EtlSqlStatement> parse(String sqlScript) {

            if (sqlScript == null || sqlScript.isBlank()) {
                throw new IllegalArgumentException("ETL SQL 脚本不能为空");
            }

            List<String> rawStatements = split(sqlScript);
            List<EtlSqlStatement> statements = new ArrayList<>(rawStatements.size());
            for (String rawStatement : rawStatements) {
                String sql = rawStatement.trim();
                String normalized = normalizeForValidation(sql);
                if (normalized.isBlank()) {
                    continue;
                }
                statements.add(new EtlSqlStatement(statements.size() + 1, sql, normalized, ""));
            }

            if (statements.isEmpty()) {
                throw new IllegalArgumentException("ETL SQL 脚本不能为空");
            }
            return statements;
        }

        static void validate(List<EtlSqlStatement> statements) {

            List<EtlSqlStatement> validated = new ArrayList<>(statements.size());
            for (EtlSqlStatement statement : statements) {
                String type = classify(statement);
                validated.add(new EtlSqlStatement(statement.index(), statement.sql(), statement.normalized(), type));
            }
            statements.clear();
            statements.addAll(validated);
        }

        private static String classify(EtlSqlStatement statement) {

            List<String> tokens = tokens(statement.normalized());
            if (tokens.isEmpty()) {
                throw new IllegalArgumentException("ETL SQL 第 " + statement.index() + " 条为空");
            }

            String first = tokens.getFirst();
            if ("SELECT".equals(first) || ("WITH".equals(first) && tokens.contains("SELECT"))) {
                return "SELECT";
            }
            if ("INSERT".equals(first)) {
                return classifyInsert(statement.index(), tokens);
            }
            if ("CREATE".equals(first)) {
                return classifyCreate(statement.index(), tokens);
            }

            throw new IllegalArgumentException("ETL SQL 第 " + statement.index() + " 条不允许执行: " + first);
        }

        private static String classifyInsert(int index, List<String> tokens) {

            if (tokens.size() < 3 || !"INTO".equals(tokens.get(1))) {
                throw new IllegalArgumentException("ETL SQL 第 " + index + " 条 INSERT 只允许 INSERT INTO ... SELECT");
            }
            if (tokens.contains("OVERWRITE")) {
                throw new IllegalArgumentException("ETL SQL 第 " + index + " 条不允许执行 INSERT OVERWRITE");
            }
            int selectIndex = tokens.indexOf("SELECT");
            int valuesIndex = tokens.indexOf("VALUES");
            if (valuesIndex >= 0 && (selectIndex < 0 || valuesIndex < selectIndex)) {
                throw new IllegalArgumentException("ETL SQL 第 " + index + " 条 INSERT INTO 只允许 SELECT，不允许 VALUES");
            }
            if (selectIndex < 0) {
                throw new IllegalArgumentException("ETL SQL 第 " + index + " 条 INSERT INTO 只允许 SELECT");
            }
            return "INSERT_INTO_SELECT";
        }

        private static String classifyCreate(int index, List<String> tokens) {

            if (tokens.size() >= 2 && "CATALOG".equals(tokens.get(1))) {
                return "CREATE_CATALOG";
            }
            if (tokens.size() >= 3 && "MATERIALIZED".equals(tokens.get(1)) && "VIEW".equals(tokens.get(2))) {
                return "CREATE_MATERIALIZED_VIEW";
            }
            if (tokens.size() >= 2 && "TABLE".equals(tokens.get(1))) {
                return "CREATE_TABLE";
            }
            if (tokens.size() >= 3 && "EXTERNAL".equals(tokens.get(1)) && "TABLE".equals(tokens.get(2))) {
                return "CREATE_TABLE";
            }
            throw new IllegalArgumentException("ETL SQL 第 " + index + " 条 CREATE 只允许 TABLE、MATERIALIZED VIEW、CATALOG");
        }

        private static List<String> split(String sqlScript) {

            List<String> statements = new ArrayList<>();
            StringBuilder current = new StringBuilder();
            State state = State.NORMAL;

            for (int i = 0; i < sqlScript.length(); i++) {
                char c = sqlScript.charAt(i);
                char next = i + 1 < sqlScript.length() ? sqlScript.charAt(i + 1) : '\0';

                switch (state) {
                    case NORMAL -> {
                        if (c == '-' && next == '-') {
                            current.append(c).append(next);
                            i++;
                            state = State.LINE_COMMENT;
                        } else if (c == '/' && next == '*') {
                            current.append(c).append(next);
                            i++;
                            state = State.BLOCK_COMMENT;
                        } else if (c == '\'') {
                            current.append(c);
                            state = State.SINGLE_QUOTE;
                        } else if (c == '"') {
                            current.append(c);
                            state = State.DOUBLE_QUOTE;
                        } else if (c == '`') {
                            current.append(c);
                            state = State.BACKTICK;
                        } else if (c == ';') {
                            statements.add(current.toString());
                            current.setLength(0);
                        } else {
                            current.append(c);
                        }
                    }
                    case LINE_COMMENT -> {
                        current.append(c);
                        if (c == '\n' || c == '\r') {
                            state = State.NORMAL;
                        }
                    }
                    case BLOCK_COMMENT -> {
                        current.append(c);
                        if (c == '*' && next == '/') {
                            current.append(next);
                            i++;
                            state = State.NORMAL;
                        }
                    }
                    case SINGLE_QUOTE -> {
                        current.append(c);
                        if (c == '\\' && next != '\0') {
                            current.append(next);
                            i++;
                        } else if (c == '\'' && next == '\'') {
                            current.append(next);
                            i++;
                        } else if (c == '\'') {
                            state = State.NORMAL;
                        }
                    }
                    case DOUBLE_QUOTE -> {
                        current.append(c);
                        if (c == '\\' && next != '\0') {
                            current.append(next);
                            i++;
                        } else if (c == '"' && next == '"') {
                            current.append(next);
                            i++;
                        } else if (c == '"') {
                            state = State.NORMAL;
                        }
                    }
                    case BACKTICK -> {
                        current.append(c);
                        if (c == '`' && next == '`') {
                            current.append(next);
                            i++;
                        } else if (c == '`') {
                            state = State.NORMAL;
                        }
                    }
                }
            }

            statements.add(current.toString());
            return statements;
        }

        private static String normalizeForValidation(String sql) {

            StringBuilder normalized = new StringBuilder(sql.length());
            State state = State.NORMAL;

            for (int i = 0; i < sql.length(); i++) {
                char c = sql.charAt(i);
                char next = i + 1 < sql.length() ? sql.charAt(i + 1) : '\0';

                switch (state) {
                    case NORMAL -> {
                        if (c == '-' && next == '-') {
                            normalized.append(' ');
                            i++;
                            state = State.LINE_COMMENT;
                        } else if (c == '/' && next == '*') {
                            normalized.append(' ');
                            i++;
                            state = State.BLOCK_COMMENT;
                        } else if (c == '\'') {
                            normalized.append(' ');
                            state = State.SINGLE_QUOTE;
                        } else if (c == '"') {
                            normalized.append(' ');
                            state = State.DOUBLE_QUOTE;
                        } else if (c == '`') {
                            normalized.append(' ');
                            state = State.BACKTICK;
                        } else {
                            normalized.append(c);
                        }
                    }
                    case LINE_COMMENT -> {
                        if (c == '\n' || c == '\r') {
                            normalized.append(' ');
                            state = State.NORMAL;
                        }
                    }
                    case BLOCK_COMMENT -> {
                        if (c == '*' && next == '/') {
                            normalized.append(' ');
                            i++;
                            state = State.NORMAL;
                        }
                    }
                    case SINGLE_QUOTE -> {
                        if (c == '\\' && next != '\0') {
                            i++;
                        } else if (c == '\'' && next == '\'') {
                            i++;
                        } else if (c == '\'') {
                            normalized.append(' ');
                            state = State.NORMAL;
                        }
                    }
                    case DOUBLE_QUOTE -> {
                        if (c == '\\' && next != '\0') {
                            i++;
                        } else if (c == '"' && next == '"') {
                            i++;
                        } else if (c == '"') {
                            normalized.append(' ');
                            state = State.NORMAL;
                        }
                    }
                    case BACKTICK -> {
                        if (c == '`' && next == '`') {
                            i++;
                        } else if (c == '`') {
                            normalized.append(' ');
                            state = State.NORMAL;
                        }
                    }
                }
            }

            return normalized.toString().trim();
        }

        private static List<String> tokens(String normalized) {

            String[] parts = normalized.toUpperCase(Locale.ROOT).split("[^A-Z0-9_]+");
            List<String> tokens = new ArrayList<>(parts.length);
            for (String part : parts) {
                if (!part.isBlank()) {
                    tokens.add(part);
                }
            }
            return tokens;
        }
    }

    private enum State {
        NORMAL,
        LINE_COMMENT,
        BLOCK_COMMENT,
        SINGLE_QUOTE,
        DOUBLE_QUOTE,
        BACKTICK
    }
}
