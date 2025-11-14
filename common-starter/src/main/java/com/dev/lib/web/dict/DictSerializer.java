package com.dev.lib.web.dict;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import jakarta.annotation.Resource;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class DictSerializer extends JsonSerializer<String> {

    @Resource
    private DictService dictService;

    @Override
    public void serialize(String value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        if (value == null) {
            gen.writeNull();
            return;
        }

        try {
            DictItem item = dictService.getItem(value);
            if (item != null) {
                gen.writeStartObject();
                gen.writeStringField("code", item.getCode());
                gen.writeStringField("label", item.getLabel());
                gen.writeStringField("css", item.getCss());
                gen.writeEndObject();
            } else {
                gen.writeString(value);
            }
        } catch (Exception e) {
            gen.writeString(value);
        }
    }
}