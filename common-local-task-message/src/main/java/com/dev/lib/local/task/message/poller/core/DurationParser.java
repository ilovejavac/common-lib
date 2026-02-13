package com.dev.lib.local.task.message.poller.core;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Duration 字符串解析工具
 * 支持格式: "10s", "500ms", "1m", "2h", "3d"
 */
public final class DurationParser {

    private static final Pattern DURATION_PATTERN = Pattern.compile(
        "^(\\d+)\\s*(ms|s|m|h|d)$",
        Pattern.CASE_INSENSITIVE
    );

    private DurationParser() {
        // 工具类，禁止实例化
    }

    /**
     * 解析 Duration 字符串
     * @param text 字符串格式，如 "10s", "500ms", "1m", "2h", "3d"
     * @return 解析后的 Duration
     * @throws IllegalArgumentException 如果格式不正确
     */
    public static Duration parse(String text) {
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("Duration text cannot be null or blank");
        }

        String trimmed = text.trim().toLowerCase();
        Matcher matcher = DURATION_PATTERN.matcher(trimmed);

        if (!matcher.matches()) {
            throw new IllegalArgumentException(
                "Invalid duration format: " + text +
                ". Expected format: <number><unit>, e.g., 10s, 500ms, 1m, 2h, 3d"
            );
        }

        long amount = Long.parseLong(matcher.group(1));
        String unit = matcher.group(2);

        return switch (unit) {
            case "ms" -> Duration.of(amount, ChronoUnit.MILLIS);
            case "s" -> Duration.of(amount, ChronoUnit.SECONDS);
            case "m" -> Duration.of(amount, ChronoUnit.MINUTES);
            case "h" -> Duration.of(amount, ChronoUnit.HOURS);
            case "d" -> Duration.of(amount, ChronoUnit.DAYS);
            default -> throw new IllegalArgumentException("Unknown duration unit: " + unit);
        };
    }

    /**
     * 安全解析 Duration 字符串，解析失败时返回默认值
     * @param text 字符串格式
     * @param defaultValue 默认值
     * @return 解析后的 Duration，失败时返回 defaultValue
     */
    public static Duration parseOrDefault(String text, Duration defaultValue) {
        try {
            return parse(text);
        } catch (IllegalArgumentException e) {
            return defaultValue;
        }
    }

}
