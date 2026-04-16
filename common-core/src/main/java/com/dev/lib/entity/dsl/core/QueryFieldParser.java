package com.dev.lib.entity.dsl.core;

import com.dev.lib.entity.dsl.QueryType;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 字段名解析器
 * <p>
 * 字段名格式: {targetField}{QueryType}{Sub?}
 * <p>
 * 解析顺序（从右到左）：
 * 1. Sub → 子查询标记
 * 2. QueryType → 查询类型
 * 3. 剩余部分 → 目标字段名（首字母转小写）
 * <p>
 * 示例：
 * - nameEq → field=name, type=EQ, subQuery=false
 * - logsExistsSub → field=logs, type=EXISTS, subQuery=true
 * - latestStatusEqSub → field=latestStatus, type=EQ, subQuery=true
 */
public class QueryFieldParser {

    private static final Map<String, QueryType> QUERY_TYPE_SUFFIX = new LinkedHashMap<>();

    static {
        // 长后缀优先，避免误匹配

        // EXISTS 系列
        QUERY_TYPE_SUFFIX.put(
                "NotExists",
                QueryType.NOT_EXISTS
        );
        QUERY_TYPE_SUFFIX.put(
                "Exists",
                QueryType.EXISTS
        );

        // NULL 系列
        QUERY_TYPE_SUFFIX.put(
                "IsNotNull",
                QueryType.IS_NOT_NULL
        );
        QUERY_TYPE_SUFFIX.put(
                "IsNull",
                QueryType.IS_NULL
        );

        // 字符串匹配
        QUERY_TYPE_SUFFIX.put(
                "StartWith",
                QueryType.START_WITH
        );
        QUERY_TYPE_SUFFIX.put(
                "EndWith",
                QueryType.END_WITH
        );
        QUERY_TYPE_SUFFIX.put(
                "Like",
                QueryType.LIKE
        );

        // 集合
        QUERY_TYPE_SUFFIX.put(
                "NotIn",
                QueryType.NOT_IN
        );
        QUERY_TYPE_SUFFIX.put(
                "In",
                QueryType.IN
        );

        // 比较
        QUERY_TYPE_SUFFIX.put(
                "Between",
                QueryType.BETWEEN
        );
        QUERY_TYPE_SUFFIX.put(
                "Ge",
                QueryType.GE
        );
        QUERY_TYPE_SUFFIX.put(
                "Gt",
                QueryType.GT
        );
        QUERY_TYPE_SUFFIX.put(
                "Le",
                QueryType.LE
        );
        QUERY_TYPE_SUFFIX.put(
                "Lt",
                QueryType.LT
        );
        QUERY_TYPE_SUFFIX.put(
                "Ne",
                QueryType.NE
        );
        QUERY_TYPE_SUFFIX.put(
                "Eq",
                QueryType.EQ
        );
    }

    /**
     * 解析字段名
     */
    public static ParsedField parse(String fieldName) {

        String          remaining  = fieldName;
        boolean         isSubQuery = false;
        QueryType       queryType  = QueryType.EQ;

        // 1. 解析子查询标记 (Sub)
        if (remaining.endsWith("Sub")) {
            isSubQuery = true;
            remaining = remaining.substring(
                    0,
                    remaining.length() - 3
            );
        }

        // 2. 解析查询类型后缀
        for (Map.Entry<String, QueryType> entry : QUERY_TYPE_SUFFIX.entrySet()) {
            if (remaining.endsWith(entry.getKey())) {
                queryType = entry.getValue();
                remaining = remaining.substring(
                        0,
                        remaining.length() - entry.getKey().length()
                );
                break;
            }
        }

        // 3. 目标字段名首字母转小写
        String targetField = remaining.isEmpty() ? remaining :
                             Character.toLowerCase(remaining.charAt(0)) + remaining.substring(1);

        return new ParsedField(
                targetField,
                queryType,
                isSubQuery
        );
    }

    /**
     * 解析结果
     */
    public record ParsedField(
            String targetField,
            QueryType queryType,
            boolean subQuery
    ) {}

}
