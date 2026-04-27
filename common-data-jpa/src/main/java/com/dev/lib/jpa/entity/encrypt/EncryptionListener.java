package com.dev.lib.jpa.entity.encrypt;

import com.dev.lib.entity.encrypt.Encrypt;
import com.dev.lib.entity.encrypt.EncryptionService;
import jakarta.persistence.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 多版本数据库加密解密
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EncryptionListener {

    private static final Map<Class<?>, Field[]> ENCRYPT_FIELD_CACHE = new ConcurrentHashMap<>();

    private final EncryptionService encryptionService;

    @PrePersist
    @PreUpdate
    public void encryptFields(Object entity) {

        processFields(entity, true);
    }

    @PostLoad
    public void decryptFields(Object entity) {

        processFields(entity, false);
    }

    @PostPersist
    @PostUpdate
    public void decryptAfterSave(Object entity) {

        processFields(entity, false);
    }

    private void processFields(Object entity, boolean isEncrypt) {

        for (Field field : resolveEncryptFields(entity.getClass())) {
            try {
                String value = (String) field.get(entity);
                if (value != null) {
                    String processed = isEncrypt
                                       ? encryptionService.encrypt(value)
                                       : encryptionService.decrypt(value);
                    ReflectionUtils.setField(field, entity, processed);
                }
            } catch (IllegalAccessException e) {
                log.warn("Failed to process field: " + field.getName(), e);
            }
        }
    }

    private static Field[] resolveEncryptFields(Class<?> entityClass) {

        return ENCRYPT_FIELD_CACHE.computeIfAbsent(entityClass, EncryptionListener::scanEncryptFields);
    }

    private static Field[] scanEncryptFields(Class<?> entityClass) {

        return Arrays.stream(entityClass.getDeclaredFields())
                .filter(field -> field.isAnnotationPresent(Encrypt.class))
                .peek(ReflectionUtils::makeAccessible)
                .toArray(Field[]::new);
    }

}
