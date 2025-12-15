package com.dev.lib.entity.id;

import java.util.HashMap;
import java.util.Map;

public final class IntEncoder {

    private IntEncoder() {

    }

    private static final char[] CHARS_36 = "0123456789abcdefghijklmnopqrstuvwxyz".toCharArray();

    private static final char[] CHARS_52 = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ".toCharArray();

    private static final char[] CHARS_62 =
            "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ".toCharArray();

    private static final Map<Character, Integer> INDEX_MAP_36 = buildIndexMap(CHARS_36);

    private static final Map<Character, Integer> INDEX_MAP_52 = buildIndexMap(CHARS_52);

    private static final Map<Character, Integer> INDEX_MAP_62 = buildIndexMap(CHARS_62);

    private static Map<Character, Integer> buildIndexMap(char[] chars) {

        Map<Character, Integer> map = new HashMap<>();
        for (int i = 0; i < chars.length; i++) {
            map.put(chars[i], i);
        }
        return map;
    }

    public static String encode36(long num) {

        return encode(num, CHARS_36, -1);
    }

    public static String encode36(long num, int minLen) {

        return encode(num, CHARS_36, minLen);
    }

    public static String encode52(long num) {

        return encode(num, CHARS_52, -1);
    }

    public static String encode52(long num, int minLen) {

        return encode(num, CHARS_52, minLen);
    }

    public static String encode62(long num) {

        return encode(num, CHARS_62, -1);
    }

    public static String encode62(long num, int minLen) {

        return encode(num, CHARS_62, minLen);
    }

    private static String encode(long num, char[] chars, int minLen) {

        if (num < 0) {
            throw new IllegalArgumentException("数字不能是负数");
        }
        if (num == 0) {
            return minLen > 1 ? String.valueOf(chars[0]).repeat(minLen) : String.valueOf(chars[0]);
        }
        StringBuilder sb   = new StringBuilder();
        long          temp = num;
        while (temp > 0) {
            sb.append(chars[(int) (temp % chars.length)]);
            temp /= chars.length;
        }
        // 填充
        if (minLen > sb.length()) {
            int padding = minLen - sb.length();
            sb.append(String.valueOf(chars[0]).repeat(Math.max(0, padding)));
        }
        return sb.reverse().toString();
    }

    public static long decode36(String str) {

        return decode(str, CHARS_36, INDEX_MAP_36);
    }

    public static long decode52(String str) {

        return decode(str, CHARS_52, INDEX_MAP_52);
    }

    public static long decode62(String str) {

        return decode(str, CHARS_62, INDEX_MAP_62);
    }

    private static long decode(String str, char[] chars, Map<Character, Integer> indexMap) {

        if (str == null || str.isEmpty()) {
            throw new IllegalArgumentException("字符串不能为空");
        }
        long num = 0;
        for (char ch : str.trim().toCharArray()) {
            Integer index = indexMap.get(ch);
            if (index == null) {
                throw new IllegalArgumentException("无效字符: " + ch);
            }
            num = num * chars.length + index;
        }
        return num;
    }

}