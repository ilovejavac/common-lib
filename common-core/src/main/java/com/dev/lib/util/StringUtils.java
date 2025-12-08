package com.dev.lib.util;

import org.eclipse.collections.impl.factory.Strings;
import org.eclipse.collections.impl.string.immutable.CharAdapter;

public abstract class StringUtils {
    private StringUtils() {
    }

    public static String format(String template, Object... args) {
        // 模板为null直接返回null
        if (template == null) {
            return null;
        }
        // 无参数：直接返回原模板（避免空数组处理）
        if (args == null || args.length == 0) {
            return template;
        }

        CharAdapter templateAdapter = Strings.asChars(template);
        StringBuilder sb = new StringBuilder(template.length() + args.length * 10);
        int argIndex = 0; // 当前待替换的参数索引
        int templateLength = templateAdapter.size();

        // 遍历模板字符，替换{}占位符
        for (int i = 0; i < templateLength; i++) {
            char c = templateAdapter.get(i);
            // 匹配到{，且下一个字符是}，则替换为参数
            if (c == '{' && i + 1 < templateLength && templateAdapter.get(i + 1) == '}') {
                // 参数未耗尽则替换，否则保留{}
                if (argIndex < args.length) {
                    Object arg = args[argIndex++];
                    sb.append(arg == null ? "null" : arg.toString());
                } else {
                    sb.append("{}"); // 参数不足，保留占位符
                }
                i++; // 跳过}，避免重复处理
            } else {
                sb.append(c); // 非占位符，直接追加
            }
        }
        return sb.toString();
    }

    /**
     * 判断字符串为 null/空字符串/全空白字符（空格/制表符/换行）
     *
     * @param str 待判断字符串
     * @return true=空白，false=非空白
     */
    public static boolean isBlank(String str) {
        if (str == null || str.isEmpty()) {
            return true;
        }
        // 用EC的CharAdapter遍历字符，判断是否全为空白
        CharAdapter charAdapter = Strings.asChars(str);
        return charAdapter.allSatisfy(Character::isWhitespace);
    }

    /**
     * 判断字符串非 null/非空/非全空白
     *
     * @param str 待判断字符串
     * @return true=非空白，false=空白
     */
    public static boolean isNotBlank(String str) {
        return !isBlank(str);
    }

    /**
     * 判断字符串为 null 或 空字符串（仅判空，不判空白）
     *
     * @param str 待判断字符串
     * @return true=空，false=非空
     */
    public static boolean isEmpty(String str) {
        return str == null || str.isEmpty();
    }

    /**
     * 判断字符串非 null 且 非空字符串
     *
     * @param str 待判断字符串
     * @return true=非空，false=空
     */
    public static boolean isNotEmpty(String str) {
        return !isEmpty(str);
    }

    // ===================== 空白处理（开发高频） =====================

    /**
     * 空值安全的字符串修剪：null返回null，非null则trim
     *
     * @param str 待修剪字符串
     * @return 修剪后结果
     */
    public static String trim(String str) {
        return str == null ? null : str.trim();
    }

    /**
     * 修剪后，若结果为空白则返回null，否则返回修剪后字符串
     *
     * @param str 待处理字符串
     * @return 处理后结果
     */
    public static String trimToNull(String str) {
        String trimmed = trim(str);
        return isBlank(trimmed) ? null : trimmed;
    }

    /**
     * 修剪后，若结果为空白则返回空字符串，否则返回修剪后字符串
     *
     * @param str 待处理字符串
     * @return 处理后结果
     */
    public static String trimToEmpty(String str) {
        String trimmed = trim(str);
        return isBlank(trimmed) ? "" : trimmed;
    }
}
