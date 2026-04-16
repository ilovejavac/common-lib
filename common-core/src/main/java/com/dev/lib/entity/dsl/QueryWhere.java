package com.dev.lib.entity.dsl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class QueryWhere<E> {

    private final List<LogicToken> logicTokens = new ArrayList<>();

    public <Q> QueryWhere<E> use(QueryRef<Q, ?> ref) {

        logicTokens.add(LogicToken.ref(ref.getFieldName()));
        return this;
    }

    public <Q> QueryWhere<E> and(QueryRef<Q, ?> ref) {

        appendBinaryOp(LogicTokenType.AND);
        return use(ref);
    }

    public <Q> QueryWhere<E> or(QueryRef<Q, ?> ref) {

        appendBinaryOp(LogicTokenType.OR);
        return use(ref);
    }

    public QueryWhere<E> andBegin() {

        appendBinaryOp(LogicTokenType.AND);
        logicTokens.add(LogicToken.leftParen());
        return this;
    }

    public QueryWhere<E> orBegin() {

        appendBinaryOp(LogicTokenType.OR);
        logicTokens.add(LogicToken.leftParen());
        return this;
    }

    public QueryWhere<E> end() {

        logicTokens.add(LogicToken.rightParen());
        return this;
    }

    public QueryWhere<E> clear() {

        logicTokens.clear();
        return this;
    }

    public boolean hasLogic() {

        return !logicTokens.isEmpty();
    }

    public List<LogicToken> logicTokens() {

        return Collections.unmodifiableList(logicTokens);
    }

    private void appendBinaryOp(LogicTokenType type) {

        if (logicTokens.isEmpty()) {
            return;
        }
        LogicToken last = logicTokens.getLast();
        if (last.type == LogicTokenType.AND || last.type == LogicTokenType.OR || last.type == LogicTokenType.LPAREN) {
            return;
        }
        logicTokens.add(new LogicToken(type, null));
    }

    public enum LogicTokenType {
        REF,
        AND,
        OR,
        LPAREN,
        RPAREN
    }

    public static class LogicToken {

        private final LogicTokenType type;

        private final String fieldName;

        private LogicToken(LogicTokenType type, String fieldName) {

            this.type = type;
            this.fieldName = fieldName;
        }

        public static LogicToken ref(String fieldName) {

            return new LogicToken(LogicTokenType.REF, fieldName);
        }

        public static LogicToken leftParen() {

            return new LogicToken(LogicTokenType.LPAREN, null);
        }

        public static LogicToken rightParen() {

            return new LogicToken(LogicTokenType.RPAREN, null);
        }

        public LogicTokenType type() {

            return type;
        }

        public String fieldName() {

            return fieldName;
        }
    }
}

