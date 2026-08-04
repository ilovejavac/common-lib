package com.dev.lib.util;

import com.dev.lib.config.JacksonSupport;
import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Type;
import java.util.Map;
import java.util.Objects;

public final class Jsons {

    private static final JsonMapper MAPPER = JacksonSupport.mapper();

    private Jsons() {
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

        write(MAPPER, outputStream, value);
    }

    public static <T> T parse(String json, Class<T> type) {

        Objects.requireNonNull(type, "type must not be null");
        try {
            return MAPPER.readValue(json, type);
        } catch (JacksonException e) {
            throw new IllegalArgumentException("Failed to deserialize JSON to " + type.getName(), e);
        }
    }

    public static <T> T parse(String json, TypeReference<T> typeReference) {

        Objects.requireNonNull(typeReference, "typeReference must not be null");
        try {
            return MAPPER.readValue(json, typeReference);
        } catch (JacksonException e) {
            throw new IllegalArgumentException("Failed to deserialize JSON by TypeReference", e);
        }
    }

    public static <T> T parse(String json, Type type) {

        Objects.requireNonNull(type, "type must not be null");
        try {
            return MAPPER.readValue(json, MAPPER.constructType(type));
        } catch (JacksonException e) {
            throw new IllegalArgumentException("Failed to deserialize JSON by Type", e);
        }
    }

    public static <T> T parse(InputStream inputStream, Class<T> type) throws IOException {

        Objects.requireNonNull(type, "type must not be null");
        return read(MAPPER, inputStream, type);
    }

    public static Object parse(String json) {

        return parse(json, Object.class);
    }

    public static JsonNode readTree(String json) {

        try {
            return MAPPER.readTree(json);
        } catch (JacksonException e) {
            throw new IllegalArgumentException("Failed to read JSON tree", e);
        }
    }

    public static <T> T convert(Object value, Class<T> type) {

        Objects.requireNonNull(type, "type must not be null");
        if (value == null) {
            return null;
        }
        try {
            return MAPPER.convertValue(value, type);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Failed to convert JSON value to " + type.getName(), e);
        }
    }

    public static <T> T convert(Object value, TypeReference<T> typeReference) {

        Objects.requireNonNull(typeReference, "typeReference must not be null");
        if (value == null) {
            return null;
        }
        try {
            return MAPPER.convertValue(value, typeReference);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Failed to convert JSON value by TypeReference", e);
        }
    }

    public static <T> T convert(Object value, Type type) {

        Objects.requireNonNull(type, "type must not be null");
        if (value == null) {
            return null;
        }
        try {
            return MAPPER.convertValue(value, MAPPER.constructType(type));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Failed to convert JSON value by Type", e);
        }
    }

    public static Map<String, Object> toMap(Object value) {

        return convert(value, new TypeReference<>() {
        });
    }

    private static void write(JsonMapper mapper, OutputStream outputStream, Object value) throws IOException {

        Objects.requireNonNull(outputStream, "outputStream must not be null");
        try {
            mapper.writeValue(outputStream, value);
        } catch (JacksonException e) {
            throw new IOException("Failed to write JSON", e);
        }
    }

    private static <T> T read(JsonMapper mapper, InputStream inputStream, Class<T> type) throws IOException {

        Objects.requireNonNull(inputStream, "inputStream must not be null");
        try {
            return mapper.readValue(inputStream, type);
        } catch (JacksonException e) {
            throw new IOException("Failed to read JSON as " + type.getName(), e);
        }
    }
}
