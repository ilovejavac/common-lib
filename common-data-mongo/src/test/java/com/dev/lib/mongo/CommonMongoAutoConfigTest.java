package com.dev.lib.mongo;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

class CommonMongoAutoConfigTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(CommonMongoAutoConfig.class));

    @Test
    void shouldRegisterBaseEntityCallbackWithoutRequiringEncryptionService() {

        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(MongoBaseEntityCallback.class);
            assertThat(context).doesNotHaveBean(MongoEncryptionCallback.class);
        });
    }
}
