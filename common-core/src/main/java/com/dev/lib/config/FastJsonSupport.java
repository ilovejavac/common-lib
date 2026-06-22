package com.dev.lib.config;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONReader;
import com.alibaba.fastjson2.JSONWriter;
import com.alibaba.fastjson2.filter.AfterFilter;
import com.alibaba.fastjson2.filter.Filter;
import com.alibaba.fastjson2.filter.ValueFilter;
import com.alibaba.fastjson2.reader.ObjectReaders;
import com.alibaba.fastjson2.writer.ObjectWriter;
import com.alibaba.fastjson2.writer.ObjectWriters;
import com.dev.lib.web.sensitive.Sensitive;
import com.dev.lib.web.sensitive.SensitiveType;
import com.dev.lib.web.serialize.PopulateContextHolder;
import com.dev.lib.web.serialize.PopulateField;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

public final class FastJsonSupport {

    public static final String TIME_ZONE = "Asia/Shanghai";

    public static final ZoneId ZONE_ID = ZoneId.of(TIME_ZONE);

    public static final String DATE_FORMAT = "yyyy-MM-dd HH:mm:ss";

    public static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern(DATE_FORMAT);

    public static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");

    public static final JSONReader.Feature[] READER_FEATURES = {
            JSONReader.Feature.UseBigDecimalForFloats,
            JSONReader.Feature.UseBigDecimalForDoubles,
            JSONReader.Feature.EmptyStringAsNull,
            JSONReader.Feature.SupportSmartMatch
    };

    public static final JSONWriter.Feature[] WRITER_FEATURES = {
            JSONWriter.Feature.WriteBigDecimalAsPlain,
            JSONWriter.Feature.WriteEnumUsingToString
    };

    private static final long JS_SAFE_INTEGER_MAX = 9007199254740991L;

    private static final Map<Class<?>, Map<String, SensitiveType>> SENSITIVE_FIELDS = new ConcurrentHashMap<>();

    private static final Map<Class<?>, PopulateFieldMeta[]> POPULATE_FIELDS = new ConcurrentHashMap<>();

    public static final ValueFilter COMMON_VALUE_FILTER = (object, name, value) -> {
        if (value instanceof BigDecimal decimal) {
            return decimal.setScale(6, RoundingMode.HALF_UP);
        }
        if (value instanceof Long longValue
                && (longValue > JS_SAFE_INTEGER_MAX || longValue < -JS_SAFE_INTEGER_MAX)) {
            return longValue.toString();
        }
        if (object != null && name != null && value instanceof String text) {
            SensitiveType type = SENSITIVE_FIELDS.computeIfAbsent(
                    object.getClass(),
                    FastJsonSupport::sensitiveFields
            ).get(name);
            if (type != null) {
                return mask(text, type);
            }
        }
        return value;
    };

    public static final Filter[] WRITER_FILTERS = {
            COMMON_VALUE_FILTER,
            new PopulateFieldFilter()
    };

    public static final JSONReader.Feature[] POLYMORPHIC_READER_FEATURES = {
            JSONReader.Feature.UseBigDecimalForFloats,
            JSONReader.Feature.UseBigDecimalForDoubles,
            JSONReader.Feature.EmptyStringAsNull,
            JSONReader.Feature.SupportSmartMatch,
            JSONReader.Feature.SupportAutoType,
            JSONReader.Feature.SupportClassForName
    };

    public static final JSONWriter.Feature[] POLYMORPHIC_WRITER_FEATURES = {
            JSONWriter.Feature.WriteBigDecimalAsPlain,
            JSONWriter.Feature.WriteEnumUsingToString,
            JSONWriter.Feature.WriteClassName
    };

    private static final AtomicBoolean CONFIGURED = new AtomicBoolean();

    private FastJsonSupport() {
    }

    public static void configure() {

        if (!CONFIGURED.compareAndSet(false, true)) {
            return;
        }
        JSON.configReaderDateFormat(DATE_FORMAT);
        JSON.configWriterDateFormat(DATE_FORMAT);
        JSON.configReaderZoneId(ZONE_ID);
        JSON.configWriterZoneId(ZONE_ID);
        JSON.register(BigDecimal.class, ObjectWriters.ofToBigDecimal(value ->
                ((BigDecimal) value).setScale(6, RoundingMode.HALF_UP)));
        JSON.register(LocalDateTime.class, ObjectWriters.ofToString(value ->
                DATE_TIME_FORMATTER.format((LocalDateTime) value)));
        JSON.register(LocalDateTime.class, ObjectReaders.ofString(FastJsonSupport::parseLocalDateTime));
        JSON.register(LocalDate.class, ObjectWriters.ofToString(value ->
                DATE_FORMATTER.format((LocalDate) value)));
        JSON.register(LocalDate.class, ObjectReaders.ofString(FastJsonSupport::parseLocalDate));
        JSON.register(LocalTime.class, ObjectWriters.ofToString(value ->
                TIME_FORMATTER.format((LocalTime) value)));
        JSON.register(LocalTime.class, ObjectReaders.ofString(FastJsonSupport::parseLocalTime));
        JSON.register(Instant.class, ObjectWriters.ofToString(value ->
                DATE_TIME_FORMATTER.format(((Instant) value).atZone(ZONE_ID))));
        JSON.register(Instant.class, ObjectReaders.ofString(FastJsonSupport::parseInstant));
        JSON.register(Long.class, new LongWriter());
        JSON.register(Long.TYPE, new LongWriter());
    }

    private static LocalDateTime parseLocalDateTime(String text) {

        if (text == null || text.isBlank()) {
            return null;
        }
        text = text.trim();
        if (text.contains("T")) {
            return LocalDateTime.parse(text);
        }
        return LocalDateTime.parse(text, DATE_TIME_FORMATTER);
    }

    private static LocalDate parseLocalDate(String text) {

        return text == null || text.isBlank() ? null : LocalDate.parse(text.trim(), DATE_FORMATTER);
    }

    private static LocalTime parseLocalTime(String text) {

        return text == null || text.isBlank() ? null : LocalTime.parse(text.trim(), TIME_FORMATTER);
    }

    private static Instant parseInstant(String text) {

        if (text == null || text.isBlank()) {
            return null;
        }
        text = text.trim();
        if (text.length() > 100) {
            throw new IllegalArgumentException("Date string too long: " + text.length());
        }
        if (text.contains("T")) {
            return Instant.parse(text);
        }
        return LocalDateTime.parse(text, DATE_TIME_FORMATTER).atZone(ZONE_ID).toInstant();
    }

    private static Map<String, SensitiveType> sensitiveFields(Class<?> clazz) {

        Map<String, SensitiveType> fields = new HashMap<>();
        Class<?> current = clazz;
        while (current != null && current != Object.class) {
            for (Field field : current.getDeclaredFields()) {
                Sensitive sensitive = field.getAnnotation(Sensitive.class);
                if (sensitive != null) {
                    fields.put(field.getName(), sensitive.type());
                }
            }
            current = current.getSuperclass();
        }
        return fields;
    }

    private static String mask(String value, SensitiveType type) {

        return switch (type) {
            case PHONE -> value.replaceAll(
                    "(\\d{3})\\d{4}(\\d{4})",
                    "$1****$2"
            );
            case ID_CARD -> value.replaceAll(
                    "(\\d{3})\\d{11}(\\d{4})",
                    "$1***********$2"
            );
            case EMAIL -> value.replaceAll(
                    "(\\w{1})\\w+(@.*)",
                    "$1***$2"
            );
            case NAME -> value.charAt(0) + "*".repeat(value.length() - 1);
            case BANK_CARD -> value.replaceAll(
                    "(\\d{4})\\d+(\\d{4})",
                    "$1 **** **** $2"
            );
        };
    }

    private static PopulateFieldMeta[] populateFields(Class<?> clazz) {

        Map<String, PopulateFieldMeta> fields = new HashMap<>();
        Class<?> current = clazz;
        while (current != null && current != Object.class) {
            for (Field field : current.getDeclaredFields()) {
                PopulateField populateField = field.getAnnotation(PopulateField.class);
                if (populateField == null) {
                    continue;
                }
                ReflectionUtils.makeAccessible(field);
                fields.putIfAbsent(
                        field.getName(),
                        new PopulateFieldMeta(field, populateField.loader(), field.getName() + populateField.suffix())
                );
            }
            current = current.getSuperclass();
        }
        return fields.values().toArray(PopulateFieldMeta[]::new);
    }

    private record PopulateFieldMeta(Field field, String loaderName, String outputName) {
    }

    private static final class PopulateFieldFilter extends AfterFilter {

        @Override
        public void writeAfter(Object object) {

            if (object == null) {
                return;
            }
            for (PopulateFieldMeta meta : POPULATE_FIELDS.computeIfAbsent(object.getClass(), FastJsonSupport::populateFields)) {
                Object idValue = ReflectionUtils.getField(meta.field(), object);
                if (idValue != null) {
                    writeKeyValue(meta.outputName(), PopulateContextHolder.get(meta.loaderName(), idValue));
                }
            }
        }
    }

    private static final class LongWriter implements ObjectWriter<Long> {

        @Override
        public void write(
                JSONWriter jsonWriter,
                Object object,
                Object fieldName,
                Type fieldType,
                long features
        ) {

            if (object == null) {
                jsonWriter.writeNull();
                return;
            }
            Long value = (Long) object;
            if (value > JS_SAFE_INTEGER_MAX || value < -JS_SAFE_INTEGER_MAX) {
                jsonWriter.writeString(value.toString());
                return;
            }
            jsonWriter.writeInt64(value);
        }
    }
}
