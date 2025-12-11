package com.dev.lib.web.sensitive;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.ContextualSerializer;

import java.io.IOException;

// 序列化器
public class SensitiveSerializer extends JsonSerializer<String> implements ContextualSerializer {

    private SensitiveType type;

    @Override
    public void serialize(String value, JsonGenerator gen, SerializerProvider serializers) throws IOException {

        if (value == null) {
            gen.writeNull();
            return;
        }

        String masked = switch (type) {
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

        gen.writeString(masked);
    }

    @Override
    public JsonSerializer<?> createContextual(
            SerializerProvider prov,
            BeanProperty property
    ) throws JsonMappingException {

        Sensitive annotation = property.getAnnotation(Sensitive.class);
        if (annotation != null) {
            this.type = annotation.type();
            return this;
        }
        return prov.findNullValueSerializer(property);
    }

}