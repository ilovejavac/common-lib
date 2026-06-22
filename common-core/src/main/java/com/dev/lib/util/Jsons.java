package com.dev.lib.util;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONException;
import com.alibaba.fastjson2.JSONObject;
import com.alibaba.fastjson2.TypeReference;
import com.dev.lib.config.FastJsonSupport;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Type;
import java.util.Map;

public final class Jsons {

    static {
        FastJsonSupport.configure();
    }

    private Jsons() {
    }

    public static String toJson(Object value) {

        try {
            return JSON.toJSONString(value, FastJsonSupport.WRITER_FILTERS, FastJsonSupport.WRITER_FEATURES);
        } catch (JSONException e) {
            throw new IllegalArgumentException("Failed to serialize object to JSON", e);
        }
    }

    public static byte[] toBytes(Object value) {

        try {
            return JSON.toJSONBytes(value, FastJsonSupport.WRITER_FILTERS, FastJsonSupport.WRITER_FEATURES);
        } catch (JSONException e) {
            throw new IllegalArgumentException("Failed to serialize object to JSON bytes", e);
        }
    }

    public static void write(OutputStream outputStream, Object value) throws IOException {

        JSON.writeTo(outputStream, value, FastJsonSupport.WRITER_FILTERS, FastJsonSupport.WRITER_FEATURES);
    }

    public static void writeWithType(OutputStream outputStream, Object value) throws IOException {

        JSON.writeTo(outputStream, value, FastJsonSupport.WRITER_FILTERS, FastJsonSupport.POLYMORPHIC_WRITER_FEATURES);
    }

    public static <T> T parse(String json, Class<T> type) {

        try {
            return JSON.parseObject(json, type, FastJsonSupport.READER_FEATURES);
        } catch (JSONException e) {
            throw new IllegalArgumentException("Failed to deserialize JSON to " + type.getName(), e);
        }
    }

    public static <T> T parse(String json, TypeReference<T> typeReference) {

        try {
            return JSON.parseObject(json, typeReference, FastJsonSupport.READER_FEATURES);
        } catch (JSONException e) {
            throw new IllegalArgumentException("Failed to deserialize JSON by TypeReference", e);
        }
    }

    public static <T> T parse(String json, Type type) {

        try {
            return JSON.parseObject(json, type, FastJsonSupport.READER_FEATURES);
        } catch (JSONException e) {
            throw new IllegalArgumentException("Failed to deserialize JSON by Type", e);
        }
    }

    public static <T> T parse(InputStream inputStream, Class<T> type) throws IOException {

        return JSON.parseObject(inputStream, type, FastJsonSupport.READER_FEATURES);
    }

    public static <T> T parseWithType(InputStream inputStream, Class<T> type) throws IOException {

        return JSON.parseObject(inputStream, type, FastJsonSupport.POLYMORPHIC_READER_FEATURES);
    }

    public static Object parse(String json) {

        try {
            return JSON.parse(json, FastJsonSupport.READER_FEATURES);
        } catch (JSONException e) {
            throw new IllegalArgumentException("Failed to deserialize JSON to Object", e);
        }
    }

    public static JSONObject readTree(String json) {

        try {
            return JSON.parseObject(json, FastJsonSupport.READER_FEATURES);
        } catch (JSONException e) {
            throw new IllegalArgumentException("Failed to read JSON tree", e);
        }
    }

    public static <T> T convert(Object value, Class<T> type) {

        if (value == null) {
            return null;
        }
        return JSON.parseObject(toJson(value), type, FastJsonSupport.READER_FEATURES);
    }

    public static <T> T convert(Object value, TypeReference<T> typeReference) {

        if (value == null) {
            return null;
        }
        return JSON.parseObject(toJson(value), typeReference, FastJsonSupport.READER_FEATURES);
    }

    public static <T> T convert(Object value, Type type) {

        if (value == null) {
            return null;
        }
        return JSON.parseObject(toJson(value), type, FastJsonSupport.READER_FEATURES);
    }

    public static Map<String, Object> toMap(Object value) {

        return convert(value, new TypeReference<>() {
        });
    }
}
