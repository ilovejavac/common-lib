package com.dev.lib.config;

import com.alibaba.fastjson2.JSONReader;
import com.alibaba.fastjson2.JSONWriter;
import com.alibaba.fastjson2.filter.ValueFilter;
import com.alibaba.fastjson2.reader.ObjectReader;
import com.alibaba.fastjson2.writer.ObjectWriter;

import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.Date;

public final class FastJson2Support {

    public static final ZoneId ZONE_ID = ZoneId.of("Asia/Shanghai");

    public static final String DATE_FORMAT = "yyyy-MM-dd HH:mm:ss";

    private static final DateTimeFormatter DATE_ONLY = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private static final DateTimeFormatter TIME_ONLY = DateTimeFormatter.ofPattern("HH:mm:ss");

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern(DATE_FORMAT);

    private FastJson2Support() {

    }

    // ============ Writer Features ============
    public static final JSONWriter.Feature[] WRITER_FEATURES = {
            JSONWriter.Feature.WriteBigDecimalAsPlain,
            JSONWriter.Feature.WriteEnumUsingToString,
            JSONWriter.Feature.WriteNullListAsEmpty,
            JSONWriter.Feature.SortMapEntriesByKeys,

            JSONWriter.Feature.NotWriteDefaultValue      // null 字段不输出（类似 NON_NULL）
    };
    // ============ Reader Features ============
    public static final JSONReader.Feature[] READER_FEATURES = {
            JSONReader.Feature.SupportSmartMatch,
            JSONReader.Feature.UseBigDecimalForDoubles,
            JSONReader.Feature.SupportArrayToBean,
            JSONReader.Feature.TrimString,
            JSONReader.Feature.ErrorOnNotSupportAutoType,
            JSONReader.Feature.ErrorOnEnumNotMatch,

            JSONReader.Feature.AllowUnQuotedFieldNames,   // 宽松解析
            JSONReader.Feature.IgnoreAutoTypeNotMatch     // 忽略未知字段
    };

    // ============ ValueFilter：序列化时处理 BigDecimal、Instant 和 Long ============
    public static final ValueFilter VALUE_FILTER = (obj, name, value) -> {
        if (value instanceof BigDecimal bd) {
            return bd.setScale(6, RoundingMode.HALF_UP);
        }
        if (value instanceof Instant instant) {
            return FORMATTER.format(instant.atZone(ZONE_ID));
        }
        if (value instanceof LocalDateTime ldt) {
            return FORMATTER.format(ldt);
        }
        if (value instanceof LocalDate ld) {
            return DATE_ONLY.format(ld);
        }
        if (value instanceof LocalTime lt) {
            return TIME_ONLY.format(lt);
        }
        if (value instanceof Date date) {
            return FORMATTER.format(date.toInstant().atZone(ZONE_ID));
        }
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
            return LocalDateTime.parse(
                    text,
                    FORMATTER
            ).atZone(ZONE_ID).toInstant();
        }

    }

}