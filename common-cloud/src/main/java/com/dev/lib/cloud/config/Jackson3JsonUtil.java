package com.dev.lib.cloud.config;

import com.dev.lib.config.JacksonSupport;
import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.common.json.impl.AbstractJsonUtilImpl;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.lang.reflect.Type;
import java.util.List;

@Activate(order = 100, onClass = "tools.jackson.databind.json.JsonMapper")
public class Jackson3JsonUtil extends AbstractJsonUtilImpl {

    private final JsonMapper jsonMapper = JacksonSupport.mapper();

    @Override
    public String getName() {

        return "jackson3";
    }

    @Override
    public boolean isJson(String json) {

        try {
            JsonNode node = jsonMapper.readTree(json);
            return node != null && (node.isObject() || node.isArray());
        } catch (JacksonException e) {
            return false;
        }
    }

    @Override
    public <T> T toJavaObject(String json, Type type) {

        try {
            return jsonMapper.readValue(json, jsonMapper.constructType(type));
        } catch (JacksonException e) {
            throw new IllegalArgumentException("Failed to deserialize Dubbo JSON", e);
        }
    }

    @Override
    public <T> List<T> toJavaList(String json, Class<T> type) {

        try {
            return jsonMapper.readValue(
                    json,
                    jsonMapper.getTypeFactory().constructCollectionType(List.class, type)
            );
        } catch (JacksonException e) {
            throw new IllegalArgumentException("Failed to deserialize Dubbo JSON list", e);
        }
    }

    @Override
    public String toJson(Object value) {

        try {
            return jsonMapper.writeValueAsString(value);
        } catch (JacksonException e) {
            throw new IllegalArgumentException("Failed to serialize Dubbo JSON", e);
        }
    }

    @Override
    public String toPrettyJson(Object value) {

        try {
            return jsonMapper.writerWithDefaultPrettyPrinter().writeValueAsString(value);
        } catch (JacksonException e) {
            throw new IllegalArgumentException("Failed to serialize pretty Dubbo JSON", e);
        }
    }

    @Override
    public Object convertObject(Object value, Type type) {

        return jsonMapper.convertValue(value, jsonMapper.constructType(type));
    }

    @Override
    public Object convertObject(Object value, Class<?> type) {

        return jsonMapper.convertValue(value, type);
    }
}
