package com.dev.lib.entity.id;

public class IntEncoder {

    private static final char[] CHARS_36 = "0123456789abcdefghijklmnopqrstuvwxyz".toCharArray();

    private static final char[] CHARS_52 = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ".toCharArray();

    private static final char[] CHARS_62 =
            "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ".toCharArray();

    public static String encode36(long num) {

        return encode(
                num,
                CHARS_36,
                -1
        );
    }

    public static String encode36(long num, int minLen) {

        return encode(
                num,
                CHARS_36,
                minLen
        );
    }

    public static String encode52(long num) {

        return encode(
                num,
                CHARS_52,
                -1
        );
    }

    public static String encode62(long num) {

        return encode(
                num,
                CHARS_62,
                -1
        );
    }

    private static String encode(long num, char[] chars, int minLen) {

        if (num < 0) {
            throw new IllegalArgumentException("数字不能是负数");
        }

        StringBuilder sb = new StringBuilder();
        long          temp = num;

        while (temp > 0) {
            sb.insert(
                    0,
                    chars[(int) (temp % chars.length)]
            );
            temp /= chars.length;
        }

        if (minLen > sb.length()) {
            int padding = minLen - sb.length();
            for (int i = 0; i < padding; i++) {
                sb.insert(
                        0,
                        '0'
                );
            }
        }

        return sb.toString();
    }

    public static long decode36(String str) {

        if (str == null || str.isEmpty()) {
            throw new IllegalArgumentException("字符串不能为空");
        }
        return decode(
                str.toLowerCase(),
                CHARS_36
        );
    }

    public static long decode62(String str) {

        if (str == null || str.isEmpty()) {
            throw new IllegalArgumentException("字符串不能为空");
        }
        return decode(
                str,
                CHARS_62
        );
    }

    private static long decode(String str, char[] chars) {

        long num = 0;
        for (char ch : str.trim().toCharArray()) {
            int temp;
            if (ch >= '0' && ch <= '9') {
                temp = ch - '0';
            } else if (ch >= 'A' && ch <= 'Z') {
                temp = ch - 'A' + 36;
            } else if (ch >= 'a' && ch <= 'z') {
                temp = ch - 'a' + 10;
            } else {
                throw new IllegalArgumentException("格式不正确");
            }
            num = num * chars.length + temp;
        }
        return num;
    }

}