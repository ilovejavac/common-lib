package com.dev.lib.config;

import com.dev.lib.web.serialize.PopulateContextHolder;
import com.dev.lib.web.serialize.PopulateField;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class WebMvcJacksonConfigTest {

    @AfterEach
    void tearDown() {

        PopulateContextHolder.clear();
    }

    @Test
    void shouldExcludeInternalFieldsAndAppendPopulateFields() throws Exception {

        PopulateContextHolder.preload(
                "userLoader",
                Set.of(7L),
                ids -> Map.of(7L, Map.of("username", "neo"))
        );

        SamplePayload payload = new SamplePayload();
        payload.creatorId = 7L;
        payload.reversion = 3;
        payload.deleted = true;
        payload.name = "demo";

        String json = buildMapper().writeValueAsString(payload);

        assertThat(json).contains("\"creatorId\":7");
        assertThat(json).contains("\"creatorIdInfo\":{\"username\":\"neo\"}");
        assertThat(json).contains("\"name\":\"demo\"");
        assertThat(json).doesNotContain("reversion");
        assertThat(json).doesNotContain("deleted");
    }

    private ObjectMapper buildMapper() {

        Jackson2ObjectMapperBuilder builder = new Jackson2ObjectMapperBuilder();
        Jackson2ObjectMapperBuilderCustomizer commonCustomizer = new JacksonConfig().jacksonCustomizer();
        Jackson2ObjectMapperBuilderCustomizer webCustomizer = new WebMvcConfig().webJacksonCustomizer();
        commonCustomizer.customize(builder);
        webCustomizer.customize(builder);
        return builder.build();
    }

    static class SamplePayload {

        @PopulateField(loader = "userLoader")
        public Long creatorId;

        public Integer reversion;

        public Boolean deleted;

        public String name;
    }
}
