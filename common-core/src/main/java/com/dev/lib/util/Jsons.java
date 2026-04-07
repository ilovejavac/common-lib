package com.dev.lib.util;

import com.dev.lib.config.JacksonSupport;
import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.DefaultTyping;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectReader;
import tools.jackson.databind.ObjectWriter;
import tools.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;

public final class Jsons {

    private static final JsonMapper MAPPER = buildMapper();

    private static final JsonMapper POLYMORPHIC_MAPPER = buildPolymorphicMapper();

    private Jsons() {
    }

    private static JsonMapper buildMapper() {

        JsonMapper.Builder builder = JsonMapper.builder();
        JacksonSupport.configure(builder);
        return builder.build();
    }

    private static JsonMapper buildPolymorphicMapper() {

        JsonMapper.Builder builder = MAPPER.rebuild();
        builder.activateDefaultTypingAsProperty(
                BasicPolymorphicTypeValidator.builder().allowIfSubType(Object.class).build(),
                DefaultTyping.NON_FINAL,
                "@class"
        );
        return builder.build();
    }

    public static String toJson(Object value) {

        try {
            return MAPPER.writeValueAsString(value);
        } catch (JacksonException e) {
            throw new IllegalArgumentException("Failed to serialize object to JSON", e);
        }
    }

    public static byte[] toBytes(Object value) {

        try {
            return MAPPER.writeValueAsBytes(value);
        } catch (JacksonException e) {
            throw new IllegalArgumentException("Failed to serialize object to JSON bytes", e);
        }
    }

    public static void write(OutputStream outputStream, Object value) throws IOException {

        MAPPER.writeValue(outputStream, value);
    }

    public static void writeWithType(OutputStream outputStream, Object value) throws IOException {

        POLYMORPHIC_MAPPER.writeValue(outputStream, value);
    }

    public static <T> T parse(String json, Class<T> type) {

        try {
            return MAPPER.readValue(json, type);
        } catch (JacksonException e) {
            throw new IllegalArgumentException("Failed to deserialize JSON to " + type.getName(), e);
        }
    }

    public static <T> T parse(String json, TypeReference<T> typeReference) {

        try {
            return MAPPER.readValue(json, typeReference);
        } catch (JacksonException e) {
            throw new IllegalArgumentException("Failed to deserialize JSON by TypeReference", e);
        }
    }

    public static <T> T parse(String json, JavaType javaType) {

        try {
            return MAPPER.readValue(json, javaType);
        } catch (JacksonException e) {
            throw new IllegalArgumentException("Failed to deserialize JSON by JavaType", e);
        }
    }

    public static <T> T parse(InputStream inputStream, Class<T> type) throws IOException {

        return MAPPER.readValue(inputStream, type);
    }

    public static <T> T parseWithType(InputStream inputStream, Class<T> type) throws IOException {

        return POLYMORPHIC_MAPPER.readValue(inputStream, type);
    }

    public static Object parse(String json) {

        try {
            return MAPPER.readValue(json, Object.class);
        } catch (JacksonException e) {
            throw new IllegalArgumentException("Failed to deserialize JSON to Object", e);
        }
    }

    public static JsonNode readTree(String json) {

        try {
            return MAPPER.readTree(json);
        } catch (JacksonException e) {
            throw new IllegalArgumentException("Failed to read JSON tree", e);
        }
    }

    public static <T> T convert(Object value, Class<T> type) {

        return MAPPER.convertValue(value, type);
    }

    public static <T> T convert(Object value, TypeReference<T> typeReference) {

        return MAPPER.convertValue(value, typeReference);
    }

    public static Map<String, Object> toMap(Object value) {

        return convert(value, new TypeReference<>() {
        });
    }

    public static ObjectReader readerFor(Class<?> type) {

        return MAPPER.readerFor(type);
    }

    public static ObjectWriter writerFor(Class<?> type) {

        return MAPPER.writerFor(type);
    }
}
