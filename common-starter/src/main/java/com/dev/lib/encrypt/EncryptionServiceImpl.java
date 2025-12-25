package com.dev.lib.encrypt;

import com.dev.lib.config.properties.AppSecurityProperties;
import com.dev.lib.encrypt.factory.EncryptionStrategyFactory;
import com.dev.lib.entity.encrypt.EncryptVersion;
import com.dev.lib.entity.encrypt.EncryptionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EncryptionServiceImpl implements EncryptionService {

    private final EncryptionStrategyFactory factory;

    private final AppSecurityProperties     securityProperties;

    public String encrypt(String dbValue) {

        EncryptVersion encryptVersion = securityProperties.getEncryptVersion();
        if (isEncrypted(dbValue)) {
            String version = extractVersion(dbValue);
            if (version.equals(encryptVersion.getMsg().toLowerCase())) {
                // 已用当前版本加密，不重复
                return dbValue;
            }
            // 解密后重新加密
            String plainText = decrypt(dbValue);
            return encryptWithVersion(
                    plainText,
                    encryptVersion
            );
        }
        return encryptWithVersion(
                dbValue,
                encryptVersion
        );
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

    private String encryptWithVersion(String plainText, EncryptVersion version) {

        String encrypted = factory.getStrategy(version).encrypt(plainText);
        return version + ":" + encrypted;
    }

}