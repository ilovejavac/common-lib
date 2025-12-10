package com.dev.lib.mongo;

import com.dev.lib.entity.encrypt.Encrypt;
import com.dev.lib.entity.encrypt.EncryptionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.data.mongodb.core.mapping.event.AfterConvertCallback;
import org.springframework.data.mongodb.core.mapping.event.AfterSaveCallback;
import org.springframework.data.mongodb.core.mapping.event.BeforeConvertCallback;
import org.springframework.stereotype.Component;
import org.springframework.util.ReflectionUtils;

import java.util.Arrays;

/**
 * 等效于 JPA 的 EncryptionListener
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MongoEncryptionCallback implements 
        BeforeConvertCallback<Object>,
        AfterConvertCallback<Object>,
        AfterSaveCallback<Object>,
        Ordered {

    private final EncryptionService encryptionService;

    /**
     * 保存前加密（等效 @PrePersist + @PreUpdate）
     */
    @Override
    public Object onBeforeConvert(Object entity, String collection) {
        processFields(entity, true);
        return entity;
    }

    /**
     * 查询后解密（等效 @PostLoad）
     */
    @Override
    public Object onAfterConvert(Object entity, org.bson.Document document, String collection) {
        processFields(entity, false);
        return entity;
    }

    /**
     * 保存后解密（等效 @PostPersist + @PostUpdate）
     * 保持内存中对象为明文
     */
    @Override
    public Object onAfterSave(Object entity, org.bson.Document document, String collection) {
        processFields(entity, false);
        return entity;
    }

    private void processFields(Object entity, boolean isEncrypt) {
        Arrays.stream(entity.getClass().getDeclaredFields())
                .filter(field -> field.isAnnotationPresent(Encrypt.class))
                .forEach(field -> {
                    ReflectionUtils.makeAccessible(field);
                    try {
                        String value = (String) field.get(entity);
                        if (value != null && !value.isEmpty()) {
                            String processed = isEncrypt
                                    ? encryptionService.encrypt(value)
                                    : encryptionService.decrypt(value);
                            ReflectionUtils.setField(field, entity, processed);
                        }
                    } catch (Exception e) {
                        log.warn("Failed to process field: {}", field.getName(), e);
                    }
                });
    }

    @Override
    public int getOrder() {
        return 200;
    }
}
