package com.dev.lib.util.encrypt.factory;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.dev.lib.entity.encrypt.EncryptVersion;
import com.dev.lib.util.encrypt.Encryptor;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class EncryptionStrategyFactoryTest {

    @Test
    void shouldLogLoadedEncryptionStrategiesInEnglishWithoutVersionCodes() {

        Logger logger = (Logger) LoggerFactory.getLogger(EncryptionStrategyFactory.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        try {
            new EncryptionStrategyFactory(List.of(new TestEncryptor()));

            assertThat(appender.list)
                    .extracting(ILoggingEvent::getFormattedMessage)
                    .anySatisfy(message -> assertThat(message)
                            .isEqualTo("Loaded encryption strategies: [AES]")
                            .doesNotContain("v2"));
        } finally {
            logger.detachAppender(appender);
        }
    }

    private static class TestEncryptor implements Encryptor {

        @Override
        public EncryptVersion getVersion() {

            return EncryptVersion.AES;
        }

        @Override
        public String encrypt(String value) {

            return value;
        }

        @Override
        public String decrypt(String value) {

            return value;
        }
    }
}
