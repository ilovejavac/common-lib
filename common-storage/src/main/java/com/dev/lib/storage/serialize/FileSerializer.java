package com.dev.lib.storage.serialize;

import com.dev.lib.storage.domain.service.FileService;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import jakarta.annotation.Resource;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class FileSerializer extends JsonSerializer<String> {

    @Resource
    private FileService fileService;

    @Override
    public void serialize(String value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        if (value == null) {
            gen.writeNull();
            return;
        }

        try {
            FileItem item = fileService.getItem(value);
            if (item != null) {
                gen.writeObject(item);
            } else {
                gen.writeString(value);
            }
        } catch (Exception e) {
            gen.writeString(value);
        }
    }
}