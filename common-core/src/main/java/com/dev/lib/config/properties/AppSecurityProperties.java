package com.dev.lib.config.properties;

import com.dev.lib.entity.encrypt.EncryptVersion;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Set;

@Data
@ConfigurationProperties(prefix = "app.security")
public class AppSecurityProperties {

    private EncryptVersion encryptVersion;

    private String secret;

    private Long expiration;

    private Set<String> whiteListRequest;

    private String rsaPublicKey;

    private String rsaPrivateKey;

}
