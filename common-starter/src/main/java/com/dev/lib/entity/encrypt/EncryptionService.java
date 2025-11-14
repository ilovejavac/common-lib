package com.dev.lib.entity.encrypt;

import com.dev.lib.config.properties.AppSecurityProperties;
import com.dev.lib.entity.encrypt.factory.EncryptionStrategyFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EncryptionService {
    private final EncryptionStrategyFactory factory;
    private final AppSecurityProperties securityProperties;

    public String encrypt(String dbValue) {
        EncryptVersion encryptVersion = securityProperties.getEncryptVersion();
        if (isEncrypted(dbValue)) {
            String version = extractVersion(dbValue);
            if (version.equals(encryptVersion.name().toLowerCase())) {
                // 已用当前版本加密，不重复
                return dbValue;
            }
            // 解密后重新加密
            String plainText = decrypt(dbValue);
            return encryptWithVersion(plainText, version);
        }
        return encryptWithVersion(dbValue, encryptVersion.name().toLowerCase());
    }

    public String decrypt(String dbValue) {
        if (!isEncrypted(dbValue)) {
            return dbValue;
        }
        String version = extractVersion(dbValue);
        String cipherText = dbValue.substring(version.length() + 1);
        return factory.getStrategy(version).decrypt(cipherText);
    }

    private boolean isEncrypted(String value) {
        return value != null && value.matches("^v\\d+:.+");
    }

    private String extractVersion(String value) {
        return value.substring(0, value.indexOf(':'));
    }

    private String encryptWithVersion(String plainText, String version) {
        String encrypted = factory.getStrategy(version).encrypt(plainText);
        return version + ":" + encrypted;
    }
}