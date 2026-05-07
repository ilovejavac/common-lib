package com.dev.lib.mongo;

import com.dev.lib.entity.encrypt.EncryptionService;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
public class CommonMongoAutoConfig {

    @Bean
    @ConditionalOnMissingBean
    public MongoBaseEntityCallback mongoBaseEntityCallback() {

        return new MongoBaseEntityCallback();
    }

    @Bean
    @ConditionalOnBean(EncryptionService.class)
    @ConditionalOnMissingBean
    public MongoEncryptionCallback mongoEncryptionCallback(EncryptionService encryptionService) {

        return new MongoEncryptionCallback(encryptionService);
    }
}
