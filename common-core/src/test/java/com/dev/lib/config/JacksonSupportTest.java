package com.dev.lib.config;

import com.dev.lib.util.Jsons;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.boot.jackson.JsonComponentModule;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class JacksonSupportTest {

    private final Jackson2ObjectMapperBuilderCustomizer customizer = new JacksonConfig().jacksonCustomizer();

    private final ObjectMapper objectMapper = buildMapper();

    @Test
    void shouldSerializeUsingExpectedCommonRules() throws Exception {

        Payload payload = new Payload();
        payload.amount = new BigDecimal("12.3");
        payload.createdAt = Instant.parse("2026-03-11T04:05:06Z");
        payload.largeId = 9007199254740992L;
        payload.status = Status.ENABLED;

        String json = objectMapper.writeValueAsString(payload);

        assertThat(json).contains("\"amount\":12.300000");
        assertThat(json).contains("\"createdAt\":\"2026-03-11 12:05:06\"");
        assertThat(json).contains("\"largeId\":\"9007199254740992\"");
        assertThat(json).contains("\"status\":\"enabled\"");
        assertThat(json).doesNotContain("optionalNote");
    }

    @Test
    void shouldProvideSameRulesThroughStaticJsonsUtility() {

        Payload payload = new Payload();
        payload.amount = new BigDecimal("12.3");
        payload.createdAt = Instant.parse("2026-03-11T04:05:06Z");
        payload.largeId = 9007199254740992L;
        payload.status = Status.ENABLED;

        String json = Jsons.toJson(payload);
        Payload restored = Jsons.parse(json, Payload.class);

        assertThat(json).contains("\"amount\":12.300000");
        assertThat(json).contains("\"createdAt\":\"2026-03-11 12:05:06\"");
        assertThat(restored.amount).isEqualByComparingTo("12.300000");
        assertThat(restored.createdAt).isEqualTo(Instant.parse("2026-03-11T04:05:06Z"));
    }

    private ObjectMapper buildMapper() {

        Jackson2ObjectMapperBuilder builder = new Jackson2ObjectMapperBuilder();
        builder.modulesToInstall(new JsonComponentModule());
        customizer.customize(builder);
        return builder.createXmlMapper(false).build();
    }

    enum Status {
        ENABLED;

        @Override
        public String toString() {

            return "enabled";
        }
    }

    static class Payload {

        public BigDecimal amount;

        public Instant createdAt;

        public Long largeId;

        public Status status;

        public String optionalNote;
    }
}
