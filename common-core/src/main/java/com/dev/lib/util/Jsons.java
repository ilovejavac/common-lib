package com.dev.lib.util;

import com.dev.lib.config.JacksonSupport;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;

public final class Jsons {

    private static final ObjectMapper MAPPER = buildMapper();

    private static final ObjectMapper POLYMORPHIC_MAPPER = buildPolymorphicMapper();

    private Jsons() {
    }

    private static ObjectMapper buildMapper() {

        ObjectMapper objectMapper = JsonMapper.builder().build();
        JacksonSupport.configure(objectMapper);
        return objectMapper;
    }

    private static ObjectMapper buildPolymorphicMapper() {

        ObjectMapper objectMapper = copyMapper();
        objectMapper.activateDefaultTyping(
                LaissezFaireSubTypeValidator.instance,
                ObjectMapper.DefaultTyping.NON_FINAL,
                JsonTypeInfo.As.PROPERTY
        );
        return objectMapper;
    }

    public static ObjectMapper mapper() {

        return MAPPER;
    }

    public static ObjectMapper copyMapper() {

        return MAPPER.copy();
    }

    public static ObjectMapper polymorphicMapper() {

        return POLYMORPHIC_MAPPER;
    }

    public static String toJson(Object value) {

        try {
            return MAPPER.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Failed to serialize object to JSON", e);
        }
    }

    public static byte[] toBytes(Object value) {

        try {
            return MAPPER.writeValueAsBytes(value);
        } catch (JsonProcessingException e) {
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
        } catch (IOException e) {
            throw new IllegalArgumentException("Failed to deserialize JSON to " + type.getName(), e);
        }
    }

    public static <T> T parse(String json, TypeReference<T> typeReference) {

        try {
            return MAPPER.readValue(json, typeReference);
        } catch (IOException e) {
            throw new IllegalArgumentException("Failed to deserialize JSON by TypeReference", e);
        }
    }

    public static <T> T parse(String json, JavaType javaType) {

        try {
            return MAPPER.readValue(json, javaType);
        } catch (IOException e) {
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
        } catch (IOException e) {
            throw new IllegalArgumentException("Failed to deserialize JSON to Object", e);
        }
    }

    public static JsonNode readTree(String json) {

        try {
            return MAPPER.readTree(json);
        } catch (IOException e) {
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
