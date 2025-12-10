package com.dev.lib.config;

import com.alibaba.fastjson2.JSONReader;
import com.alibaba.fastjson2.JSONWriter;
import com.alibaba.fastjson2.filter.ValueFilter;
import com.alibaba.fastjson2.reader.ObjectReader;
import com.alibaba.fastjson2.writer.ObjectWriter;

import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public final class FastJson2Support {

    public static final ZoneId ZONE_ID = ZoneId.of("Asia/Shanghai");
    public static final String DATE_FORMAT = "yyyy-MM-dd HH:mm:ss";
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern(DATE_FORMAT);

    private FastJson2Support() {
    }

    // ============ Writer Features ============
    public static final JSONWriter.Feature[] WRITER_FEATURES = {
            JSONWriter.Feature.WriteBigDecimalAsPlain,      // 防止科学计数法
            JSONWriter.Feature.WriteEnumUsingToString,      // 枚举可读性
            JSONWriter.Feature.WriteNullListAsEmpty,        // 前端友好（null -> []）
            JSONWriter.Feature.SortMapEntriesByKeys                 // 字段顺序稳定，便于调试
    };

    // ============ Reader Features ============
    public static final JSONReader.Feature[] READER_FEATURES = {
            // ✅ 必要功能
            JSONReader.Feature.SupportSmartMatch,              // 驼峰/下划线兼容
            JSONReader.Feature.UseBigDecimalForDoubles,        // 浮点数精度
            JSONReader.Feature.SupportArrayToBean,             // 单值转数组
            JSONReader.Feature.TrimString,                     // 去除空格

            // 🔒 安全配置
            JSONReader.Feature.ErrorOnNotSupportAutoType,      // 禁止 AutoType（最重要！）
            JSONReader.Feature.ErrorOnEnumNotMatch             // 枚举严格校验
    };

    // ============ 🔒 安全限制常量 ============
    /**
     * 最大嵌套深度（对标 Jackson 的 maxNestingDepth）
     * 防止深度嵌套 JSON 导致栈溢出
     */
    public static final int MAX_NESTING_DEPTH = 1000;

    /**
     * 最大字符串长度（对标 Jackson 的 maxStringLength）
     * 防止超大字符串导致内存溢出
     */
    public static final int MAX_STRING_LENGTH = 20_000_000;

    // ============ ValueFilter：序列化时处理 BigDecimal、Instant 和 Long ============
    public static final ValueFilter VALUE_FILTER = (obj, name, value) -> {
        if (value instanceof BigDecimal bd) {
            return bd.setScale(6, RoundingMode.HALF_UP);
        }
        if (value instanceof Instant instant) {
            return FORMATTER.format(instant.atZone(ZONE_ID));
        }
        // 🔒 Long 精度保护
        if (value instanceof Long l && (l > 9007199254740991L || l < -9007199254740991L)) {
            return l.toString();
        }
        return value;
    };

    // ============ Instant 自定义序列化器 ============
    public static class InstantWriter implements ObjectWriter<Instant> {
        @Override
        public void write(
                JSONWriter jsonWriter, Object object,
                Object fieldName, Type fieldType, long features
        ) {
            if (object == null) {
                jsonWriter.writeNull();
                return;
            }
            Instant instant = (Instant) object;
            jsonWriter.writeString(FORMATTER.format(instant.atZone(ZONE_ID)));
        }
    }

    // ============ Instant 自定义反序列化器 ============
    public static class InstantReader implements ObjectReader<Instant> {
        @Override
        public Instant readObject(
                JSONReader jsonReader, Type fieldType,
                Object fieldName, long features
        ) {
            if (jsonReader.nextIfNull()) {
                return null;
            }
            String text = jsonReader.readString();
            if (text == null || text.isBlank()) {
                return null;
            }

            // 🔒 安全：限制字符串长度
            if (text.length() > 100) {
                throw new IllegalArgumentException("Date string too long: " + text.length());
            }

            text = text.trim();
            // 兼容 ISO 格式
            if (text.contains("T")) {
                return Instant.parse(text);
            }
            return LocalDateTime.parse(text, FORMATTER).atZone(ZONE_ID).toInstant();
        }
    }
}