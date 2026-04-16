package com.dev.lib.entity.dsl.core;

import com.dev.lib.entity.dsl.QueryWhere;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * 通用逻辑编排器：按 SQL 优先级 (AND > OR) 计算 DslQuery 逻辑 token。
 */
public final class LogicComposer {

    private static final int POSTFIX_CACHE_MAX_SIZE = 4096;

    private static final Map<String, List<QueryWhere.LogicToken>> POSTFIX_CACHE = new ConcurrentHashMap<>();

    private LogicComposer() {

    }

    public interface Combiner<T> {

        T and(T left, T right);

        T or(T left, T right);
    }

    public static <T> T compose(
            List<QueryWhere.LogicToken> logicTokens,
            Function<String, T> refResolver,
            Combiner<T> combiner
    ) {

        if (logicTokens == null || logicTokens.isEmpty()) {
            return null;
        }

        List<QueryWhere.LogicToken> postfix = toPostfixCached(logicTokens);
        Deque<T> stack = new java.util.LinkedList<>();

        for (QueryWhere.LogicToken token : postfix) {
            switch (token.type()) {
                case REF -> stack.push(refResolver.apply(token.fieldName()));
                case AND, OR -> {
                    T right = stack.isEmpty() ? null : stack.pop();
                    T left = stack.isEmpty() ? null : stack.pop();
                    stack.push(combine(left, right, token.type(), combiner));
                }
                default -> {
                }
            }
        }

        return stack.isEmpty() ? null : stack.pop();
    }

    static List<QueryWhere.LogicToken> toPostfixCached(List<QueryWhere.LogicToken> tokens) {

        String cacheKey = buildCacheKey(tokens);
        List<QueryWhere.LogicToken> cached = POSTFIX_CACHE.get(cacheKey);
        if (cached != null) {
            return cached;
        }

        List<QueryWhere.LogicToken> computed = Collections.unmodifiableList(toPostfix(tokens));
        List<QueryWhere.LogicToken> existed = POSTFIX_CACHE.putIfAbsent(cacheKey, computed);
        if (POSTFIX_CACHE.size() > POSTFIX_CACHE_MAX_SIZE) {
            evictHalf();
        }
        return existed == null ? computed : existed;
    }

    private static List<QueryWhere.LogicToken> toPostfix(List<QueryWhere.LogicToken> tokens) {

        List<QueryWhere.LogicToken> output = new ArrayList<>();
        Deque<QueryWhere.LogicToken> operators = new ArrayDeque<>();

        for (QueryWhere.LogicToken token : tokens) {
            switch (token.type()) {
                case REF -> output.add(token);
                case AND, OR -> {
                    while (!operators.isEmpty() && isOperator(operators.peek()) &&
                            precedence(operators.peek()) >= precedence(token)) {
                        output.add(operators.pop());
                    }
                    operators.push(token);
                }
                case LPAREN -> operators.push(token);
                case RPAREN -> {
                    while (!operators.isEmpty() && operators.peek().type() != QueryWhere.LogicTokenType.LPAREN) {
                        output.add(operators.pop());
                    }
                    if (!operators.isEmpty() && operators.peek().type() == QueryWhere.LogicTokenType.LPAREN) {
                        operators.pop();
                    }
                }
            }
        }

        while (!operators.isEmpty()) {
            QueryWhere.LogicToken op = operators.pop();
            if (op.type() == QueryWhere.LogicTokenType.LPAREN || op.type() == QueryWhere.LogicTokenType.RPAREN) {
                continue;
            }
            output.add(op);
        }

        return output;
    }

    private static String buildCacheKey(List<QueryWhere.LogicToken> tokens) {

        StringBuilder builder = new StringBuilder(tokens.size() * 16);
        for (QueryWhere.LogicToken token : tokens) {
            builder.append(token.type().name()).append(':');
            if (token.type() == QueryWhere.LogicTokenType.REF && token.fieldName() != null) {
                builder.append(token.fieldName());
            }
            builder.append(';');
        }
        return builder.toString();
    }

    private static void evictHalf() {

        int target = POSTFIX_CACHE_MAX_SIZE / 2;
        POSTFIX_CACHE.keySet().stream()
                .sorted(Comparator.naturalOrder())
                .limit(target)
                .forEach(POSTFIX_CACHE::remove);
    }

    static void clearCacheForTest() {

        POSTFIX_CACHE.clear();
    }

    static int cacheSizeForTest() {

        return POSTFIX_CACHE.size();
    }

    private static boolean isOperator(QueryWhere.LogicToken token) {

        return token.type() == QueryWhere.LogicTokenType.AND || token.type() == QueryWhere.LogicTokenType.OR;
    }

    private static int precedence(QueryWhere.LogicToken token) {

        return token.type() == QueryWhere.LogicTokenType.AND ? 2 : 1;
    }

    private static <T> T combine(T left, T right, QueryWhere.LogicTokenType operator, Combiner<T> combiner) {

        if (left == null) {
            return right;
        }
        if (right == null) {
            return left;
        }
        return operator == QueryWhere.LogicTokenType.AND ? combiner.and(left, right) : combiner.or(left, right);
    }
}
