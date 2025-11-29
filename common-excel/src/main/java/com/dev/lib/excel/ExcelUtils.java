package com.dev.lib.excel;

import org.springframework.core.MethodParameter;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Excel 工具类
 */
public final class ExcelUtils {
    
    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\$\\{(\\w+)}");
    
    private static final Map<String, String> PLACEHOLDER_SUPPLIERS = Map.of(
        "date", DateTimeFormatter.ofPattern("yyyyMMdd").format(LocalDateTime.now()),
        "datetime", DateTimeFormatter.ofPattern("yyyyMMddHHmmss").format(LocalDateTime.now()),
        "timestamp", String.valueOf(System.currentTimeMillis())
    );
    
    private ExcelUtils() {}
    
    /**
     * 从方法参数或返回类型中提取泛型类型
     */
    public static Class<?> extractGenericType(MethodParameter parameter) {
        Type type = parameter.getGenericParameterType();
        if (type instanceof ParameterizedType pt) {
            Type[] args = pt.getActualTypeArguments();
            if (args.length > 0 && args[0] instanceof Class<?> clazz) {
                return clazz;
            }
        }
        throw new IllegalArgumentException("无法解析泛型类型: " + parameter);
    }
    
    /**
     * 从方法返回类型提取泛型
     */
    public static Class<?> extractReturnGenericType(MethodParameter returnType) {
        Type type = returnType.getGenericParameterType();
        if (type instanceof ParameterizedType pt) {
            Type[] args = pt.getActualTypeArguments();
            if (args.length > 0 && args[0] instanceof Class<?> clazz) {
                return clazz;
            }
        }
        throw new IllegalArgumentException("无法解析返回类型泛型: " + returnType);
    }
    
    /**
     * 解析文件名模板
     */
    public static String resolveFileName(String template) {
        Matcher matcher = PLACEHOLDER_PATTERN.matcher(template);
        StringBuilder result = new StringBuilder();
        while (matcher.find()) {
            String key = matcher.group(1);
            String replacement = PLACEHOLDER_SUPPLIERS.getOrDefault(key, matcher.group(0));
            matcher.appendReplacement(result, replacement);
        }
        matcher.appendTail(result);
        return result.toString();
    }
}