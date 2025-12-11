package com.dev.lib.jpa.entity.encrypt;

import com.dev.lib.entity.encrypt.Encrypt;
import com.dev.lib.entity.encrypt.EncryptionService;
import jakarta.persistence.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.ReflectionUtils;

import java.util.Arrays;

/**
 * 多版本数据库加密解密
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EncryptionListener {

    private final EncryptionService encryptionService;

    @PrePersist
    @PreUpdate
    public void encryptFields(Object entity) {

        processFields(
                entity,
                true
        );
    }

    @PostLoad
    public void decryptFields(Object entity) {

        processFields(
                entity,
                false
        );
    }

    @PostPersist
    @PostUpdate
    public void decryptAfterSave(Object entity) {

        processFields(
                entity,
                false
        );
    }

    private void processFields(Object entity, boolean isEncrypt) {

        Arrays.stream(entity.getClass().getDeclaredFields())
                .filter(field -> field.isAnnotationPresent(Encrypt.class))
                .forEach(field -> {
                    ReflectionUtils.makeAccessible(field);
                    try {
                        String value = (String) field.get(entity);
                        if (value != null) {
                            String processed = isEncrypt
                                               ? encryptionService.encrypt(value)
                                               : encryptionService.decrypt(value);
                            ReflectionUtils.setField(
                                    field,
                                    entity,
                                    processed
                            );
                        }
                    } catch (IllegalAccessException e) {
                        log.warn(
                                "Failed to process field: " + field.getName(),
                                e
                        );
                    }
                });
    }

}