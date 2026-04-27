package com.dev.lib.config.properties;

import com.dev.lib.entity.encrypt.EncryptVersion;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Set;

@Data
@ConfigurationProperties(prefix = "app.security")
public class AppSecurityProperties {

    private EncryptVersion encryptVersion;

    private Aes aes = new Aes();

    private Base64 base64 = new Base64();

    private Rsa rsa = new Rsa();

    private Custom custom = new Custom();

    private Jwt jwt = new Jwt();

    private Long expiration;

    private Set<String> whiteListRequest;

    @Data
    public static class Aes {

        private String secret;
    }

    @Data
    public static class Base64 {

        private Boolean enabled;
    }

    @Data
    public static class Rsa {

        private Boolean enabled;

        private String publicKey;

        private String privateKey;
    }

    @Data
    public static class Custom {

        private Boolean enabled;
    }

    @Data
    public static class Jwt {

        private String secret;
    }

}
