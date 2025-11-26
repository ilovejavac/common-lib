package com.dev.lib.entity.dsl.core;

import com.dev.lib.entity.dsl.QueryType;

import java.util.LinkedHashMap;
import java.util.Map;

public class QueryFieldParser {
    
    private static final Map<String, QueryType> SUFFIX_MAP = new LinkedHashMap<>();
    
    static {
        // 长后缀优先,避免 IsNull 被 In 误匹配
        SUFFIX_MAP.put("IsNotNull", QueryType.IS_NOT_NULL);
        SUFFIX_MAP.put("IsNull", QueryType.IS_NULL);
        SUFFIX_MAP.put("StartWith", QueryType.START_WITH);
        SUFFIX_MAP.put("EndWith", QueryType.END_WITH);
        SUFFIX_MAP.put("NotIn", QueryType.NOT_IN);
        SUFFIX_MAP.put("Like", QueryType.LIKE);
        SUFFIX_MAP.put("Ge", QueryType.GE);
        SUFFIX_MAP.put("Gt", QueryType.GT);
        SUFFIX_MAP.put("Le", QueryType.LE);
        SUFFIX_MAP.put("Lt", QueryType.LT);
        SUFFIX_MAP.put("Ne", QueryType.NE);
        SUFFIX_MAP.put("In", QueryType.IN);
        SUFFIX_MAP.put("Eq", QueryType.EQ);
    }
    
    public static ParsedField parse(String fieldName) {
        for (Map.Entry<String, QueryType> entry : SUFFIX_MAP.entrySet()) {
            if (fieldName.endsWith(entry.getKey())) {
                String target = fieldName.substring(0, fieldName.length() - entry.getKey().length());
                // 首字母转小写: Name -> name
                target = Character.toLowerCase(target.charAt(0)) + target.substring(1);
                return new ParsedField(target, entry.getValue());
            }
        }
        // 无后缀默认EQ
        return new ParsedField(fieldName, QueryType.EQ);
    }
    
    public record ParsedField(String targetField, QueryType queryType) {}
}