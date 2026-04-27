package com.dev.lib.util.encrypt;

import com.dev.lib.config.properties.AppSecurityProperties;
import com.dev.lib.entity.encrypt.EncryptVersion;
import com.dev.lib.entity.encrypt.EncryptionService;
import com.dev.lib.util.encrypt.factory.EncryptionStrategyFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class EncryptionServiceImpl implements EncryptionService, InitializingBean {

    private final EncryptionStrategyFactory factory;

    private final AppSecurityProperties     securityProperties;

    private EncryptVersion encryptVersion;

    @Override
    public void afterPropertiesSet() {

        EncryptVersion configuredVersion = securityProperties.getEncryptVersion();
        if (configuredVersion != null) {
            this.encryptVersion = configuredVersion;
            return;
        }

        Map<String, Encryptor> strategies = factory.getAllStrategies();
        if (strategies.size() == 1) {
            this.encryptVersion = strategies.values().iterator().next().getVersion();
            return;
        }

        if (strategies.size() > 1) {
            throw new IllegalStateException(
                    "app.security.encrypt-version must be configured when multiple encryption strategies are registered: "
                            + strategies.values().stream()
                                    .map(strategy -> strategy.getVersion().name())
                                    .toList()
            );
        }
    }

    public String encrypt(String dbValue) {

        EncryptVersion encryptVersion = resolveEncryptVersion();
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
        return version.getMsg() + ":" + encrypted;
    }

    private EncryptVersion resolveEncryptVersion() {

        if (encryptVersion == null) {
            throw new IllegalStateException(
                    "app.security.encrypt-version must be configured when no encryption strategy is registered"
            );
        }
        return encryptVersion;
    }

}
